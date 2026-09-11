@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.auth.JwtService
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.Users
import cz.b2brental.domain.ContractId
import cz.b2brental.domain.PaymentId
import cz.b2brental.domain.toCzkMoney
import cz.b2brental.models.DocumentPdfResponse
import cz.b2brental.models.PaymentActionResponse
import cz.b2brental.models.PaymentResponse
import cz.b2brental.utils.ConflictException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.NotFoundException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Služba pro správu a evidenci plateb.
 * @property clock časový zdroj pro deterministické testy; ve výrobě systemDefaultZone().
 */
public class PaymentService(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /** Přepne nezaplacené platby po splatnosti na stav overdue */
    internal fun refreshOverdue() {
        val today: LocalDate = LocalDate.now(clock)
        transaction {
            Payments.update({
                (Payments.status eq PaymentStatus.unpaid) and (Payments.dueDate less today)
            }) {
                it[status] = PaymentStatus.overdue
            }
        }
    }

    /** Seznam plateb s volitelným filtrem podle smlouvy a rolí volajícího.
     * @param role role volajícího ("admin", "manager", "client")
     * @param callerCompanyId id firmy volajícího; null pro platformové uživatele
     * @param contractId volitelný filtr podle smlouvy
     * @param callerScope scope tokenu volajícího – "platform" nebo "tenant"
     */
    public fun list(
        role: String,
        callerCompanyId: Long?,
        contractId: ContractId?,
        callerScope: String,
    ): List<PaymentResponse> {
        refreshOverdue()
        return transaction {
            val query = Payments.selectAll()
            when (role) {
                "admin", "manager" -> {
                    if (callerScope == JwtService.SCOPE_TENANT) {
                        val compId: Long =
                            callerCompanyId
                                ?: throw ForbiddenException("Manažer nemá přiřazenou společnost")
                        val companyContractIds: List<EntityID<Long>> = getCompanyContractIds(compId)
                        if (applyCompanyContractFilter(query, companyContractIds, contractId)) {
                            return@transaction emptyList()
                        }
                    } else {
                        if (contractId != null) {
                            query.where { Payments.contractId eq contractId.value }
                        }
                    }
                }

                "client" -> {
                    val compId: Long =
                        callerCompanyId
                            ?: throw ForbiddenException("Klient nemá přiřazenou společnost")
                    val companyContractIds: List<EntityID<Long>> = getCompanyContractIds(compId)

                    if (applyCompanyContractFilter(query, companyContractIds, contractId)) {
                        return@transaction emptyList()
                    }
                }

                else -> throw ForbiddenException("Role nemá přístup k platbám")
            }
            query.map(::toResponse)
        }
    }

    /** Označení platby jako zaplacené.
     * @param id identifikátor platby
     * @param role role volajícího ("admin", "manager", "client")
     * @param callerCompanyId id firmy volajícího; null pro platformové uživatele
     * @param callerScope scope tokenu volajícího – "platform" nebo "tenant"
     */
    public fun pay(
        id: PaymentId,
        role: String,
        callerCompanyId: Long?,
        callerScope: String,
    ): PaymentActionResponse =
        transaction {
            val row = getPayment(id)

            if (role == "client" || callerScope == JwtService.SCOPE_TENANT) {
                val contractIdValue: Long = row[Payments.contractId].value
                validateContractOwnership(contractIdValue, callerCompanyId)
            }

            // Klient smí označit platbu jako zaplacenou nejdříve v den splatnosti
            if (role == "client" && row[Payments.dueDate] > LocalDate.now(clock)) {
                throw ConflictException("Platbu lze označit jako zaplacenou nejdříve v den splatnosti")
            }

            if (row[Payments.status] == PaymentStatus.paid) {
                throw ConflictException("Platba již byla zaplacena")
            }

            val now: Instant = Instant.now(clock)
            Payments.update({ Payments.id eq id.value }) {
                it[status] = PaymentStatus.paid
                it[paidAt] = now
            }

            PaymentActionResponse(id.value, PaymentStatus.paid, now)
        }

    /** Najde platbu podle id; vyhodí NotFoundException, pokud neexistuje.
     * @param id identifikátor platby
     */
    private fun getPayment(
        id: PaymentId,
    ): ResultRow =
        Payments
            .selectAll()
            .where { Payments.id eq id.value }
            .singleOrNull()
            ?: throw NotFoundException("Platba nenalezena")

    /** Získá seznam id smluv přiřazených k firmě.
     * @param compId identifikátor firmy
     */
    private fun getCompanyContractIds(
        compId: Long,
    ): List<EntityID<Long>> =
        RentalContracts
            .selectAll()
            .where { RentalContracts.companyId eq compId }
            .map { it[RentalContracts.id] }

    /** Aplikuje filtr podle smluv firmy do dotazu; vrací true, pokud má metoda vrátit prázdný seznam.
     * @param query dotaz na platby
     * @param companyContractIds seznam id smluv firmy
     * @param contractId volitelný filtr podle konkrétní smlouvy
     */
    private fun applyCompanyContractFilter(
        query: org.jetbrains.exposed.sql.Query,
        companyContractIds: List<EntityID<Long>>,
        contractId: ContractId?,
    ): Boolean {
        if (contractId != null) {
            val contractBelongsToCompany: Boolean = companyContractIds.any { it.value == contractId.value }
            if (!contractBelongsToCompany) {
                throw NotFoundException("Smlouva nenalezena")
            }
            query.where { Payments.contractId eq contractId.value }
        } else {
            if (companyContractIds.isEmpty()) {
                return true
            }
            query.where { Payments.contractId inList companyContractIds }
        }
        return false
    }

    /** Získá id firmy vlastníka smlouvy; vyhodí NotFoundException, pokud smlouva nepatří firmě volajícího.
     * @param contractIdValue identifikátor smlouvy
     * @param callerCompanyId id firmy volajícího
     */
    private fun validateContractOwnership(
        contractIdValue: Long,
        callerCompanyId: Long?,
    ) {
        val contractOwnerCompanyId: Long? =
            RentalContracts
                .selectAll()
                .where { RentalContracts.id eq contractIdValue }
                .map { it[RentalContracts.companyId].value }
                .singleOrNull()
        if (contractOwnerCompanyId != callerCompanyId) {
            throw NotFoundException("Platba nenalezena")
        }
    }

    /** Mapování řádku DB na odpověď o platbě.
     * @param row řádek výsledku dotazu z tabulky Payments
     */
    private fun toResponse(row: ResultRow): PaymentResponse =
        PaymentResponse(
            id = row[Payments.id].value,
            contractId = row[Payments.contractId].value,
            period = row[Payments.period],
            amount = row[Payments.amount].toCzkMoney(),
            dueDate = row[Payments.dueDate],
            status = row[Payments.status],
            paidAt = row[Payments.paidAt],
        )

    /**
     * Idempotentní získání nebo vytvoření záznamu PDF faktury k platbě.
     * Klient smí pouze faktury plateb své společnosti.
     * @param id identifikátor platby
     * @param role role volajícího ("admin", "manager", "client")
     * @param callerCompanyId id firmy volajícího; null pro platformové uživatele
     * @param callerUserId identifikátor uživatele, autor záznamu dokumentu
     * @param callerScope scope tokenu volajícího – "platform" nebo "tenant"
     */
    public fun pdfDocument(
        id: PaymentId,
        role: String,
        callerCompanyId: Long?,
        callerUserId: Long,
        callerScope: String,
    ): DocumentPdfResponse =
        transaction {
            val payment = getPayment(id)

            if (role == "client" || callerScope == JwtService.SCOPE_TENANT) {
                val contractIdValue: Long = payment[Payments.contractId].value
                validateContractOwnership(contractIdValue, callerCompanyId)
            }

            val existing =
                Documents
                    .selectAll()
                    .where {
                        (Documents.type eq DocumentType.invoice) and
                            (Documents.entityType eq "payment") and
                            (Documents.entityId eq id.value)
                    }.singleOrNull()

            if (existing != null) {
                return@transaction DocumentPdfResponse(existing[Documents.id].value)
            }

            val docId =
                Documents.insertAndGetId {
                    it[type] = DocumentType.invoice
                    it[entityType] = "payment"
                    it[entityId] = id.value
                    it[authorId] = EntityID(callerUserId, Users)
                }

            DocumentPdfResponse(docId.value)
        }
}
