@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Companies
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.Equipment
import cz.b2brental.db.HistoryEvents
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.TicketStatus
import cz.b2brental.db.Users
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.domain.TicketId
import cz.b2brental.models.AssignRequest
import cz.b2brental.models.DocumentPdfResponse
import cz.b2brental.models.ResolveRequest
import cz.b2brental.models.TicketActionResponse
import cz.b2brental.models.TicketCreateRequest
import cz.b2brental.models.TicketResponse
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ConflictException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.NotFoundException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

/**
 * Služba pro správu servisních požadavků a oprav
 * @property aiService služba AI pro diagnostiku a vysvětlení verdiktu
 * @property knowledgeBaseService vyhledávací služba znalostní báze (RAG)
 */
public class TicketService(
    private val aiService: AiService,
    private val knowledgeBaseService: KnowledgeBaseService,
) {
    /**
     * Vytvoření nového tiketu klientem s AI diagnostikou a hodnocením záruky
     * @param callerCompanyId id společnosti volajícího klienta
     * @param callerUserId id volajícího uživatele
     * @param req data nového tiketu (vybavení, popis, foto)
     * @return vytvořený tiket s výsledkem AI diagnostiky a verdiktem záruky
     */
    public suspend fun create(
        callerCompanyId: Long,
        callerUserId: Long,
        req: TicketCreateRequest,
    ): TicketResponse {
        if (req.description.length < MIN_DESCRIPTION_LENGTH) {
            throw BadRequestException("Popis musí mít alespoň $MIN_DESCRIPTION_LENGTH znaků")
        }

        if (req.photoBase64 != null) {
            val decoded: ByteArray =
                try {
                    Base64.getDecoder().decode(req.photoBase64)
                } catch (_: IllegalArgumentException) {
                    throw BadRequestException("Fotografie není platný Base64 řetězec")
                }
            if (decoded.size > MAX_PHOTO_BYTES) {
                throw BadRequestException("Fotografie je příliš velká (max. 4 MB)")
            }
        }

        // FÁZE 1: Načtení dat z DB v krátké transakci
        val prelude: WarrantyPrelude =
            transaction {
                Equipment.selectAll().where { Equipment.id eq req.equipmentId.value }.singleOrNull()
                    ?: throw NotFoundException("Vybavení nenalezeno")

                val activeContractIds =
                    RentalContracts
                        .selectAll()
                        .where {
                            (RentalContracts.companyId eq callerCompanyId) and (RentalContracts.status eq ContractStatus.active)
                        }.map { it[RentalContracts.id].value }

                val equipmentContractId =
                    (ContractItems innerJoin RentalContracts)
                        .selectAll()
                        .where {
                            (ContractItems.equipmentId eq req.equipmentId.value) and
                                (RentalContracts.companyId eq callerCompanyId) and
                                (RentalContracts.status eq ContractStatus.active)
                        }.singleOrNull()
                        ?.get(RentalContracts.id)
                        ?.value

                if (equipmentContractId == null || equipmentContractId !in activeContractIds) {
                    throw ConflictException("Vybavení není v aktivní smlouvě této společnosti")
                }

                val eqRow = Equipment.selectAll().where { Equipment.id eq req.equipmentId.value }.single()
                val contract = RentalContracts.selectAll().where { RentalContracts.id eq equipmentContractId }.single()
                buildWarrantyPrelude(
                    categoryId = eqRow[Equipment.categoryId].value,
                    equipmentModel = eqRow[Equipment.model],
                    contractStartDate = contract[RentalContracts.startDate],
                )
            }

        // FÁZE 2: Deterministické hodnocení a AI volání mimo transakci
        val evaluation: WarrantyEvaluation =
            if (!prelude.hasRule) {
                WarrantyEvaluation(
                    WarrantyVerdict.review_required,
                    "Není záruční pravidlo pro kategorii — vyžaduje ruční posouzení",
                )
            } else {
                WarrantyService.evaluate(
                    contractStartDate = prelude.contractStartDate,
                    warrantyMonths = prelude.warrantyMonths,
                    description = req.description,
                    excludedCauses = prelude.excludedCauses,
                    today = LocalDate.now(),
                )
            }

        val kb =
            knowledgeBaseService.retrieve(
                KnowledgeQuery(
                    text = req.description,
                    categoryName = prelude.categoryName,
                    equipmentModel = prelude.equipmentModel,
                    errorCode = null,
                ),
            )
        val diagnosis: DiagnosisResult = aiService.diagnose(req.description, req.photoBase64, kb)
        val explanation: String = aiService.explainVerdict(evaluation.verdict, evaluation.reason, kb)

        // FÁZE 3: Uložení tiketu v krátké transakci
        return transaction {
            val ticketIdEntity =
                ServiceTickets.insertAndGetId {
                    it[equipmentId] = EntityID(req.equipmentId.value, Equipment)
                    it[companyId] = EntityID(callerCompanyId, Companies)
                    it[description] = req.description
                    it[photoBase64] = req.photoBase64
                    it[severity] = diagnosis.severity
                    it[warrantyVerdict] = evaluation.verdict
                    it[warrantyReason] = explanation
                    it[aiRecommendation] = diagnosis.recommendation
                    it[status] = TicketStatus.new
                }

            HistoryEvents.insert {
                it[entityType] = "ticket"
                it[entityId] = ticketIdEntity.value
                it[eventType] = "ticket_created"
                it[message] = "Tiket vytvořen"
                it[authorId] = EntityID(callerUserId, Users)
            }

            getInternal(ticketIdEntity.value)
        }
    }

    /**
     * Seznam tiketů dle role volajícího
     * @param callerUserId id volajícího uživatele
     * @param role role volajícího (admin/manager/technician/client)
     * @param callerCompanyId id společnosti volajícího klienta nebo null
     * @return seznam tiketů dostupných volajícímu podle jeho role
     */
    public fun list(
        callerUserId: Long,
        role: String,
        callerCompanyId: Long?,
    ): List<TicketResponse> =
        transaction {
            val query = ServiceTickets.selectAll()
            when (role) {
                "admin", "manager" -> Unit
                "technician" -> query.where { ServiceTickets.technicianId eq callerUserId }
                "client" -> {
                    val compId: Long =
                        callerCompanyId
                            ?: throw ForbiddenException("Klient nemá přiřazenou společnost")
                    query.where { ServiceTickets.companyId eq compId }
                }
                else -> throw ForbiddenException("Role nemá přístup k tiketům")
            }
            query.map(::toResponse)
        }

    /**
     * Detail konkrétního tiketu s kontrolou přístupu
     * @param id identifikátor tiketu
     * @param callerUserId id volajícího uživatele
     * @param role role volajícího (admin/manager/technician/client)
     * @param callerCompanyId id společnosti volajícího klienta nebo null
     * @return detail tiketu
     */
    public fun get(
        id: TicketId,
        callerUserId: Long,
        role: String,
        callerCompanyId: Long?,
    ): TicketResponse =
        transaction {
            val row =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Tiket nenalezen")

            when (role) {
                "admin", "manager" -> Unit
                "technician" -> {
                    if (row[ServiceTickets.technicianId]?.value != callerUserId) {
                        throw ForbiddenException("Tiket není přiřazen vám")
                    }
                }
                "client" -> {
                    if (row[ServiceTickets.companyId].value != callerCompanyId) {
                        throw ForbiddenException("Nemáte přístup k tomuto tiketu")
                    }
                }
                else -> throw ForbiddenException("Nemáte přístup k tomuto tiketu")
            }

            toResponse(row)
        }

    /**
     * Přiřazení technika k tiketu (manager/admin)
     * @param id identifikátor tiketu
     * @param callerUserId id volajícího uživatele (autor záznamu v historii)
     * @param req požadavek s id technika
     * @return nový stav tiketu (assigned)
     */
    public fun assign(
        id: TicketId,
        callerUserId: Long,
        req: AssignRequest,
    ): TicketActionResponse =
        transaction {
            val row =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Tiket nenalezen")

            if (row[ServiceTickets.status] != TicketStatus.new) {
                throw ConflictException("Přiřadit lze pouze tiket ve stavu new")
            }

            val techExists: Boolean =
                Users
                    .selectAll()
                    .where {
                        (Users.id eq req.technicianId.value) and (Users.role eq ROLE_TECHNICIAN)
                    }.count() > 0L
            if (!techExists) {
                throw BadRequestException("Technik nenalezen")
            }

            ServiceTickets.update({ ServiceTickets.id eq id.value }) {
                it[technicianId] = EntityID(req.technicianId.value, Users)
                it[status] = TicketStatus.assigned
            }

            HistoryEvents.insert {
                it[entityType] = "ticket"
                it[entityId] = id.value
                it[eventType] = "ticket_assigned"
                it[message] = "Technik přiřazen k tiketu"
                it[authorId] = EntityID(callerUserId, Users)
            }

            TicketActionResponse(id.value, TicketStatus.assigned)
        }

    /**
     * Zahájení prací na tiketu technikem
     * @param id identifikátor tiketu
     * @param callerUserId id volajícího technika (musí být přiřazen k tiketu)
     * @return nový stav tiketu (in_progress)
     */
    public fun start(
        id: TicketId,
        callerUserId: Long,
    ): TicketActionResponse =
        transaction {
            val row =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Tiket nenalezen")

            if (row[ServiceTickets.technicianId]?.value != callerUserId) {
                throw ForbiddenException("Tiket není přiřazen vám")
            }

            if (row[ServiceTickets.status] != TicketStatus.assigned) {
                throw ConflictException("Zahájit lze pouze tiket ve stavu assigned")
            }

            ServiceTickets.update({ ServiceTickets.id eq id.value }) {
                it[status] = TicketStatus.in_progress
            }

            HistoryEvents.insert {
                it[entityType] = "ticket"
                it[entityId] = id.value
                it[eventType] = "ticket_started"
                it[message] = "Zahájeny práce na tiketu"
                it[authorId] = EntityID(callerUserId, Users)
            }

            TicketActionResponse(id.value, TicketStatus.in_progress)
        }

    /**
     * Vyřešení tiketu technikem se záznamem servisního protokolu
     * @param id identifikátor tiketu
     * @param callerUserId id volajícího technika (musí být přiřazen k tiketu)
     * @param req výsledek řešení (repaired/replaced/not_covered) a poznámky
     * @return nový stav tiketu (resolved) a id dokumentu protokolu
     */
    public fun resolve(
        id: TicketId,
        callerUserId: Long,
        req: ResolveRequest,
    ): TicketActionResponse =
        transaction {
            val row =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Tiket nenalezen")

            if (row[ServiceTickets.technicianId]?.value != callerUserId) {
                throw ForbiddenException("Tiket není přiřazen vám")
            }

            if (row[ServiceTickets.status] != TicketStatus.in_progress) {
                throw ConflictException("Vyřešit lze pouze tiket ve stavu in_progress")
            }

            val validResults = setOf("repaired", "replaced", "not_covered")
            if (req.result !in validResults) {
                throw BadRequestException("Neplatný výsledek řešení: ${req.result}")
            }

            val now: Instant = Instant.now()
            ServiceTickets.update({ ServiceTickets.id eq id.value }) {
                it[status] = TicketStatus.resolved
                it[resolution] = req.result
                it[resolvedAt] = now
            }

            val existingDoc =
                Documents
                    .selectAll()
                    .where {
                        (Documents.type eq DocumentType.service_report) and
                            (Documents.entityType eq "ticket") and
                            (Documents.entityId eq id.value)
                    }.singleOrNull()

            val docId: Long =
                existingDoc?.get(Documents.id)?.value
                    ?: Documents
                        .insertAndGetId {
                            it[type] = DocumentType.service_report
                            it[entityType] = "ticket"
                            it[entityId] = id.value
                            it[authorId] = EntityID(callerUserId, Users)
                        }.value

            HistoryEvents.insert {
                it[entityType] = "ticket"
                it[entityId] = id.value
                it[eventType] = "ticket_resolved"
                it[message] = "Tiket vyřešen: ${req.result}"
                it[authorId] = EntityID(callerUserId, Users)
            }

            TicketActionResponse(id.value, TicketStatus.resolved, docId)
        }

    /**
     * Interní načtení tiketu podle id
     * @param ticketIdValue číselný identifikátor tiketu
     * @return odpověď o tiketu
     */
    private fun getInternal(ticketIdValue: Long): TicketResponse {
        val row =
            ServiceTickets
                .selectAll()
                .where { ServiceTickets.id eq ticketIdValue }
                .singleOrNull()
                ?: throw NotFoundException("Tiket nenalezen")
        return toResponse(row)
    }

    /**
     * Mapování řádku DB na odpověď o tiketu
     * @param row řádek tabulky ServiceTickets
     * @return odpověď o tiketu včetně modelu vybavení a názvu firmy
     */
    private fun toResponse(row: ResultRow): TicketResponse {
        val eqId: Long = row[ServiceTickets.equipmentId].value
        val eqModel: String =
            Equipment
                .selectAll()
                .where { Equipment.id eq eqId }
                .map { it[Equipment.model] }
                .singleOrNull() ?: "Neznámé vybavení"

        val compId: Long = row[ServiceTickets.companyId].value
        val compName: String =
            Companies
                .selectAll()
                .where { Companies.id eq compId }
                .map { it[Companies.name] }
                .singleOrNull() ?: "Neznámá firma"

        return TicketResponse(
            id = row[ServiceTickets.id].value,
            equipmentId = eqId,
            equipmentModel = eqModel,
            companyName = compName,
            description = row[ServiceTickets.description],
            photoBase64 = row[ServiceTickets.photoBase64],
            severity = row[ServiceTickets.severity],
            status = row[ServiceTickets.status],
            warrantyVerdict = row[ServiceTickets.warrantyVerdict],
            warrantyReason = row[ServiceTickets.warrantyReason],
            aiRecommendation = row[ServiceTickets.aiRecommendation],
            technicianId = row[ServiceTickets.technicianId]?.value,
            resolution = row[ServiceTickets.resolution],
            createdAt = row[ServiceTickets.createdAt],
            resolvedAt = row[ServiceTickets.resolvedAt],
        )
    }

    /**
     * Idempotentní získání nebo vytvoření záznamu PDF servisní zprávy k vyřešenému tiketu.
     * Klient smí pouze tikety své společnosti, technik pouze tikety přiřazené jemu.
     * @param ticketId identifikátor tiketu
     * @param role role volajícího (technician/client)
     * @param callerCompanyId id společnosti volajícího klienta nebo null
     * @param callerUserId id volajícího uživatele
     * @return odpověď s dokumentem PDF servisní zprávy
     */
    public fun pdfDocument(
        ticketId: Long,
        role: String,
        callerCompanyId: Long?,
        callerUserId: Long,
    ): DocumentPdfResponse =
        transaction {
            val ticket =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.id eq ticketId }
                    .singleOrNull() ?: throw NotFoundException("Tiket nenalezen")

            when (role) {
                "client" -> {
                    val ownerCompanyId: Long = ticket[ServiceTickets.companyId].value
                    if (callerCompanyId != ownerCompanyId) {
                        throw ForbiddenException("Nemáte přístup k tomuto tiketu")
                    }
                }
                "technician" -> {
                    val assignedTechId: Long? = ticket[ServiceTickets.technicianId]?.value
                    if (assignedTechId != callerUserId) {
                        throw ForbiddenException("Nemáte přístup k tomuto tiketu")
                    }
                }
                "manager", "admin" -> Unit
                else -> throw ForbiddenException("Role nemá přístup")
            }

            if (ticket[ServiceTickets.status] != TicketStatus.resolved) {
                throw ConflictException("Servisní zprávu lze generovat pouze pro vyřešený tiket")
            }

            val existing =
                Documents
                    .selectAll()
                    .where {
                        (Documents.type eq DocumentType.service_report) and
                            (Documents.entityType eq "ticket") and
                            (Documents.entityId eq ticketId)
                    }.singleOrNull()

            if (existing != null) {
                return@transaction DocumentPdfResponse(existing[Documents.id].value)
            }

            val docId =
                Documents.insertAndGetId {
                    it[type] = DocumentType.service_report
                    it[entityType] = "ticket"
                    it[entityId] = ticketId
                    it[authorId] = EntityID(callerUserId, Users)
                }

            DocumentPdfResponse(docId.value)
        }

    /** Konstanty služby */
    public companion object {
        /** Minimální délka popisu závady. */
        public const val MIN_DESCRIPTION_LENGTH: Int = 10

        /** Maximální velikost fotografie po dekódování (4 MB). */
        public const val MAX_PHOTO_BYTES: Int = 4 * 1024 * 1024

        private const val ROLE_TECHNICIAN: String = "technician"
    }
}
