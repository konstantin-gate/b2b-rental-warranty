@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Companies
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.Users
import cz.b2brental.domain.ContractId
import cz.b2brental.domain.toCzkMoney
import cz.b2brental.domain.toDbBigDecimal
import cz.b2brental.models.ContractActionResponse
import cz.b2brental.models.ContractCreateRequest
import cz.b2brental.models.ContractItemResponse
import cz.b2brental.models.ContractResponse
import cz.b2brental.models.DocumentPdfResponse
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ConflictException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.NotFoundException
import org.javamoney.moneta.Money
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate

/** Služba pro správu nájemních smluv */
public class ContractService {
    /** Vytvoření nové smlouvy klientem (stav draft) */
    public fun create(
        callerCompanyId: Long,
        req: ContractCreateRequest,
    ): ContractResponse {
        if (req.months !in 1..36) {
            throw BadRequestException("Doba nájmu musí být v rozmezí 1 až 36 měsíců")
        }
        if (req.startDate.isBefore(LocalDate.now())) {
            throw BadRequestException("Datum zahájení nesmí být v minulosti")
        }

        val uniqueEquipmentIds: Set<Long> = req.equipmentIds.map { it.value }.toSet()
        if (uniqueEquipmentIds.size != req.equipmentIds.size) {
            throw BadRequestException("Duplicitní vybavení v požadavku")
        }

        return transaction {
            val rates = mutableListOf<Money>()
            for (eqId in req.equipmentIds) {
                val row =
                    Equipment
                        .selectAll()
                        .where { Equipment.id eq eqId.value }
                        .singleOrNull()
                        ?: throw NotFoundException("Vybavení s id ${eqId.value} nebylo nalezeno")

                if (row[Equipment.status] != EquipmentStatus.available) {
                    throw ConflictException("Vybavení s id ${eqId.value} není dostupné k pronájmu")
                }
                rates.add(row[Equipment.monthlyRate].toCzkMoney())
            }

            val pricing: PricingResult = PricingService.calc(rates, req.months)

            val contractIdEntity =
                RentalContracts.insertAndGetId {
                    it[companyId] = EntityID(callerCompanyId, Companies)
                    it[startDate] = req.startDate
                    it[endDate] = req.startDate.plusMonths(req.months.toLong())
                    it[months] = req.months
                    it[monthlyAmount] = pricing.monthlyAmount.toDbBigDecimal()
                    it[deposit] = pricing.deposit.toDbBigDecimal()
                    it[totalAmount] = pricing.totalAmount.toDbBigDecimal()
                    it[deliveryAddress] = req.deliveryAddress
                    it[status] = ContractStatus.draft
                }

            for (eqId in req.equipmentIds) {
                ContractItems.insert {
                    it[contractId] = contractIdEntity
                    it[equipmentId] = EntityID(eqId.value, Equipment)
                }
            }

            getInternal(contractIdEntity.value)
        }
    }

    /** Seznam smluv dle role volajícího */
    public fun list(
        role: String,
        callerCompanyId: Long?,
    ): List<ContractResponse> =
        transaction {
            val query = RentalContracts.selectAll()
            when (role) {
                "admin", "manager" -> Unit
                "client" -> {
                    val compId =
                        callerCompanyId
                            ?: throw ForbiddenException("Klient nemá přiřazenou společnost")
                    query.where { RentalContracts.companyId eq compId }
                }
                else -> throw ForbiddenException("Role nemá přístup k nájemním smlouvám")
            }
            query.map(::toResponse)
        }

    /** Získání detailu smlouvy */
    public fun get(
        id: ContractId,
        role: String,
        callerCompanyId: Long?,
    ): ContractResponse =
        transaction {
            val contract: ContractResponse = getInternal(id.value)
            if (role == "client" && contract.companyId != callerCompanyId) {
                throw ForbiddenException("Nemáte přístup k této smlouvě")
            }
            if (role != "admin" && role != "manager" && role != "client") {
                throw ForbiddenException("Nemáte přístup k této smlouvě")
            }
            contract
        }

    /** Schválení smlouvy manažerem/adminem: draft → active, vybavení → rented, graf plateb */
    public fun approve(id: ContractId): ContractActionResponse =
        transaction {
            val row =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Smlouva nenalezena")

            if (row[RentalContracts.status] != ContractStatus.draft) {
                throw ConflictException("Lze schválit pouze smlouvu ve stavu draft")
            }

            val startDate: LocalDate = row[RentalContracts.startDate]
            val months: Int = row[RentalContracts.months]
            val monthlyAmount: java.math.BigDecimal = row[RentalContracts.monthlyAmount]

            RentalContracts.update({ RentalContracts.id eq id.value }) {
                it[status] = ContractStatus.active
                it[endDate] = startDate.plusMonths(months.toLong())
            }

            val equipmentEntityIds =
                ContractItems
                    .selectAll()
                    .where { ContractItems.contractId eq id.value }
                    .map { it[ContractItems.equipmentId] }

            Equipment.update({ Equipment.id inList equipmentEntityIds }) {
                it[status] = EquipmentStatus.rented
            }

            for (period in 1..months) {
                Payments.insert {
                    it[contractId] = EntityID(id.value, RentalContracts)
                    it[Payments.period] = period
                    it[amount] = monthlyAmount
                    it[dueDate] = startDate.plusMonths(period.toLong())
                    it[status] = PaymentStatus.unpaid
                }
            }

            ContractActionResponse(id.value, ContractStatus.active, months)
        }

    /** Zamítnutí smlouvy: draft → rejected */
    public fun reject(id: ContractId): ContractActionResponse =
        transaction {
            val row =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Smlouva nenalezena")

            if (row[RentalContracts.status] != ContractStatus.draft) {
                throw ConflictException("Lze zamítnout pouze smlouvu ve stavu draft")
            }

            RentalContracts.update({ RentalContracts.id eq id.value }) {
                it[status] = ContractStatus.rejected
            }

            ContractActionResponse(id.value, ContractStatus.rejected)
        }

    /** Získání nebo vytvoření PDF dokumentu smlouvy */
    public fun pdfDocument(
        id: ContractId,
        role: String,
        callerCompanyId: Long?,
        callerUserId: Long,
    ): DocumentPdfResponse = createOrGetContractDoc(id, DocumentType.rental_contract, role, callerCompanyId, callerUserId)

    /** Získání nebo vytvoření předávacího protokolu (akceptačního aktu) */
    public fun acceptanceActDocument(
        id: ContractId,
        role: String,
        callerCompanyId: Long?,
        callerUserId: Long,
    ): DocumentPdfResponse = createOrGetContractDoc(id, DocumentType.acceptance_act, role, callerCompanyId, callerUserId)

    /** Získání nebo vytvoření protokolu o vrácení zařízení */
    public fun returnActDocument(
        id: ContractId,
        role: String,
        callerCompanyId: Long?,
        callerUserId: Long,
    ): DocumentPdfResponse = createOrGetContractDoc(id, DocumentType.return_act, role, callerCompanyId, callerUserId)

    /**
     * Idempotentní získání nebo vytvoření záznamu dokumentu smlouvy.
     * Dokument lze generovat výhradně pro aktivní smlouvu; klient smí pouze dokumenty své společnosti.
     */
    private fun createOrGetContractDoc(
        id: ContractId,
        docType: DocumentType,
        role: String,
        callerCompanyId: Long?,
        callerUserId: Long,
    ): DocumentPdfResponse =
        transaction {
            val row =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.id eq id.value }
                    .singleOrNull() ?: throw NotFoundException("Smlouva nenalezena")

            if (role == "client" && row[RentalContracts.companyId].value != callerCompanyId) {
                throw ForbiddenException("Nemáte přístup k této smlouvě")
            }

            if (row[RentalContracts.status] != ContractStatus.active) {
                throw ConflictException("Dokument lze generovat pouze pro aktivní smlouvu")
            }

            val existing =
                Documents
                    .selectAll()
                    .where {
                        (Documents.type eq docType) and
                            (Documents.entityType eq "contract") and
                            (Documents.entityId eq id.value)
                    }.singleOrNull()

            if (existing != null) {
                return@transaction DocumentPdfResponse(existing[Documents.id].value)
            }

            val newDocId =
                Documents.insertAndGetId {
                    it[type] = docType
                    it[entityType] = "contract"
                    it[entityId] = id.value
                    it[authorId] = EntityID(callerUserId, Users)
                }

            DocumentPdfResponse(newDocId.value)
        }

    /** Interní načtení smlouvy podle id */
    private fun getInternal(contractIdValue: Long): ContractResponse {
        val row =
            RentalContracts
                .selectAll()
                .where { RentalContracts.id eq contractIdValue }
                .singleOrNull()
                ?: throw NotFoundException("Smlouva nenalezena")
        return toResponse(row)
    }

    /** Mapování řádku DB na odpověď o smlouvě */
    private fun toResponse(row: ResultRow): ContractResponse {
        val compId: Long = row[RentalContracts.companyId].value
        val compName: String =
            Companies
                .selectAll()
                .where { Companies.id eq compId }
                .map { it[Companies.name] }
                .singleOrNull() ?: "Neznámá firma"

        val contractIdValue: Long = row[RentalContracts.id].value
        val items: List<ContractItemResponse> =
            ContractItems
                .selectAll()
                .where { ContractItems.contractId eq contractIdValue }
                .map { itemRow ->
                    ContractItemResponse(
                        equipmentId = itemRow[ContractItems.equipmentId].value,
                        model =
                            Equipment
                                .selectAll()
                                .where { Equipment.id eq itemRow[ContractItems.equipmentId].value }
                                .map { it[Equipment.model] }
                                .singleOrNull() ?: "Neznámé vybavení",
                    )
                }

        return ContractResponse(
            id = contractIdValue,
            companyId = compId,
            companyName = compName,
            status = row[RentalContracts.status],
            startDate = row[RentalContracts.startDate],
            endDate = row[RentalContracts.endDate],
            months = row[RentalContracts.months],
            monthlyAmount = row[RentalContracts.monthlyAmount].toCzkMoney(),
            deposit = row[RentalContracts.deposit].toCzkMoney(),
            totalAmount = row[RentalContracts.totalAmount].toCzkMoney(),
            deliveryAddress = row[RentalContracts.deliveryAddress],
            items = items,
        )
    }
}
