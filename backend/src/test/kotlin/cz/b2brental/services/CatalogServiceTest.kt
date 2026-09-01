@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Companies
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.Users
import cz.b2brental.db.WarrantyRules
import cz.b2brental.domain.EquipmentCategoryId
import cz.b2brental.domain.EquipmentId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class CatalogServiceTest {
    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:catalogtest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction {
            SchemaUtils.create(Companies, Users, EquipmentCategories, Equipment, WarrantyRules)
            val catId =
                EquipmentCategories.insertAndGetId {
                    it[name] = "Chladicí vitrína"
                    it[icon] = "❄️"
                }
            Equipment.insertAndGetId {
                it[categoryId] = catId
                it[model] = "CoolMax 3000"
                it[serialNumber] = "SN-12345"
                it[price] = BigDecimal("50000.00")
                it[monthlyRate] = BigDecimal("4500.00")
                it[status] = EquipmentStatus.available
            }
            Equipment.insertAndGetId {
                it[categoryId] = catId
                it[model] = "CoolMax 5000"
                it[serialNumber] = "SN-67890"
                it[price] = BigDecimal("80000.00")
                it[monthlyRate] = BigDecimal("7000.00")
                it[status] = EquipmentStatus.rented
            }
        }
    }

    @Test
    fun listAllTest() {
        val service = CatalogService()
        val result = service.list(null, null)
        assertEquals(2, result.size)
    }

    @Test
    fun listByStatusAvailableTest() {
        val service = CatalogService()
        val result = service.list(null, "available")
        assertEquals(1, result.size)
        assertEquals("CoolMax 3000", result[0].model)
    }

    @Test
    fun getByIdTest() {
        val service = CatalogService()
        val eqId =
            transaction {
                Equipment
                    .selectAll()
                    .where { Equipment.model eq "CoolMax 3000" }
                    .first()[Equipment.id]
                    .value
            }
        val result = service.get(EquipmentId(eqId))
        assertEquals("CoolMax 3000", result.model)
        assertEquals("SN-12345", result.serialNumber)
    }

    @Test
    fun listByCategoryTest() {
        val service = CatalogService()
        val catId =
            transaction {
                EquipmentCategories.selectAll().first()[EquipmentCategories.id].value
            }
        val result = service.list(EquipmentCategoryId(catId), null)
        assertEquals(2, result.size)
    }
}
