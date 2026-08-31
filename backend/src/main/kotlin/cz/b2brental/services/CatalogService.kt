@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.ContractItems
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.ServiceTickets
import cz.b2brental.domain.EquipmentCategoryId
import cz.b2brental.domain.EquipmentId
import cz.b2brental.domain.toCzkMoney
import cz.b2brental.domain.toDbBigDecimal
import cz.b2brental.models.CatalogItemResponse
import cz.b2brental.models.CatalogUpsertRequest
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ConflictException
import cz.b2brental.utils.NotFoundException
import org.javamoney.moneta.Money
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/** Služba pro správu katalogu vybavení */
public class CatalogService {
    /** Vrátí seznam položek s volitelným filtrem podle kategorie a stavu */
    public fun list(
        categoryId: EquipmentCategoryId?,
        status: String?,
    ): List<CatalogItemResponse> {
        val statusEnum: EquipmentStatus? =
            if (status == null) {
                null
            } else {
                runCatching { EquipmentStatus.valueOf(status) }
                    .getOrElse { throw BadRequestException("Neplatný status: $status") }
            }

        return transaction {
            val query = Equipment.selectAll()
            if (categoryId != null && statusEnum != null) {
                query.where {
                    (Equipment.categoryId eq categoryId.value) and (Equipment.status eq statusEnum)
                }
            } else if (categoryId != null) {
                query.where { Equipment.categoryId eq categoryId.value }
            } else if (statusEnum != null) {
                query.where { Equipment.status eq statusEnum }
            }
            query.map(::toResponse)
        }
    }

    /** Vrátí detail konkrétního vybavení podle ID */
    public fun get(id: EquipmentId): CatalogItemResponse =
        transaction {
            Equipment
                .selectAll()
                .where { Equipment.id eq id.value }
                .map(::toResponse)
                .singleOrNull()
                ?: throw NotFoundException("Vybavení nenalezeno")
        }

    /** Vytvoří novou položku v katalogu se stavem available */
    public fun create(req: CatalogUpsertRequest): Long {
        validateMoney(req.price, req.monthlyRate)
        return transaction {
            requireCategoryExists(req.categoryId.value)
            if (equipmentSerialExists(req.serialNumber)) {
                throw ConflictException("Číslo série již existuje")
            }

            Equipment
                .insertAndGetId {
                    it[Equipment.categoryId] = EntityID(req.categoryId.value, EquipmentCategories)
                    it[Equipment.model] = req.model
                    it[Equipment.serialNumber] = req.serialNumber
                    it[Equipment.price] = req.price.toDbBigDecimal()
                    it[Equipment.monthlyRate] = req.monthlyRate.toDbBigDecimal()
                    it[Equipment.description] = req.description
                    it[Equipment.photoUrl] = req.photoUrl
                    it[Equipment.status] = EquipmentStatus.available
                }.value
        }
    }

    /** Aktualizuje existující položku v katalogu */
    public fun update(
        id: EquipmentId,
        req: CatalogUpsertRequest,
    ) {
        validateMoney(req.price, req.monthlyRate)
        transaction {
            val existing =
                Equipment
                    .selectAll()
                    .where { Equipment.id eq id.value }
                    .singleOrNull()
                    ?: throw NotFoundException("Vybavení nenalezeno")

            requireCategoryExists(req.categoryId.value)

            val currentSerial: String = existing[Equipment.serialNumber]
            if (req.serialNumber != currentSerial && equipmentSerialExists(req.serialNumber)) {
                throw ConflictException("Číslo série již existuje")
            }

            Equipment.update({ Equipment.id eq id.value }) {
                it[Equipment.categoryId] = EntityID(req.categoryId.value, EquipmentCategories)
                it[Equipment.model] = req.model
                it[Equipment.serialNumber] = req.serialNumber
                it[Equipment.price] = req.price.toDbBigDecimal()
                it[Equipment.monthlyRate] = req.monthlyRate.toDbBigDecimal()
                it[Equipment.description] = req.description
                it[Equipment.photoUrl] = req.photoUrl
            }
        }
    }

    /** Smaže položku, pokud není svázána se smlouvou ani servisním tiketem */
    public fun delete(id: EquipmentId) {
        transaction {
            val exists =
                Equipment
                    .selectAll()
                    .where { Equipment.id eq id.value }
                    .count() > 0L
            if (!exists) {
                throw NotFoundException("Vybavení nenalezeno")
            }

            val isInAnyContract =
                ContractItems
                    .selectAll()
                    .where { ContractItems.equipmentId eq id.value }
                    .count() > 0L
            if (isInAnyContract) {
                throw ConflictException("Vybavení je svázáno se smlouvou")
            }

            val isInAnyTicket =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.equipmentId eq id.value }
                    .count() > 0L
            if (isInAnyTicket) {
                throw ConflictException("Vybavení je svázáno se servisním tiketem")
            }

            Equipment.deleteWhere { Equipment.id eq id.value }
        }
    }

    /** Cena a měsíční sazba nesmí být záporné */
    private fun validateMoney(
        price: Money,
        monthlyRate: Money,
    ) {
        if (price.isNegative || monthlyRate.isNegative) {
            throw BadRequestException("Cena a měsíční sazba nesmí být záporné")
        }
    }

    /** Kategorie musí existovat */
    private fun requireCategoryExists(categoryIdValue: Long) {
        val exists =
            EquipmentCategories
                .selectAll()
                .where { EquipmentCategories.id eq categoryIdValue }
                .count() > 0L
        if (!exists) {
            throw BadRequestException("Kategorie nenalezena")
        }
    }

    /** Kontrola duplicitního sériového čísla */
    private fun equipmentSerialExists(serialNumber: String): Boolean =
        Equipment
            .selectAll()
            .where { Equipment.serialNumber eq serialNumber }
            .count() > 0L

    /** Mapování řádku DB na odpověď katalogu */
    private fun toResponse(row: ResultRow): CatalogItemResponse {
        val catId: Long = row[Equipment.categoryId].value
        val categoryName: String =
            EquipmentCategories
                .selectAll()
                .where { EquipmentCategories.id eq catId }
                .map { it[EquipmentCategories.name] }
                .singleOrNull() ?: "Neznámá kategorie"

        return CatalogItemResponse(
            id = row[Equipment.id].value,
            categoryId = catId,
            categoryName = categoryName,
            model = row[Equipment.model],
            serialNumber = row[Equipment.serialNumber],
            price = row[Equipment.price].toCzkMoney(),
            monthlyRate = row[Equipment.monthlyRate].toCzkMoney(),
            description = row[Equipment.description],
            photoUrl = row[Equipment.photoUrl],
            status = row[Equipment.status],
        )
    }
}
