@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.auth.JwtService
import cz.b2brental.db.Companies
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Documents
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.Users
import cz.b2brental.db.WarrantyRules
import cz.b2brental.domain.ContractId
import cz.b2brental.utils.NotFoundException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContractServiceTest {
    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:contractsvc${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction {
            SchemaUtils.create(
                Companies,
                Users,
                EquipmentCategories,
                Equipment,
                RentalContracts,
                ContractItems,
                Payments,
                ServiceTickets,
                WarrantyRules,
                Documents,
            )
        }
    }

    @Test
    fun listEmptyTest() {
        val service = ContractService(NotificationService())
        val result = service.list("admin", null, JwtService.SCOPE_PLATFORM)
        assertEquals(0, result.size)
    }

    @Test
    fun pdfDocumentNotFoundTest() {
        val service = ContractService(NotificationService())
        assertFailsWith<NotFoundException> {
            service.pdfDocument(ContractId(99999), "admin", null, 1L, JwtService.SCOPE_PLATFORM)
        }
    }

    @Test
    fun pdfDocumentClientForbiddenTest() {
        val contractId =
            transaction {
                val compId =
                    Companies.insertAndGetId {
                        it[name] = "Test s.r.o."
                        it[inn] = "28745001"
                        it[address] = "Praha"
                    }
                RentalContracts
                    .insertAndGetId {
                        it[companyId] = compId
                        it[startDate] = LocalDate.parse("2026-01-01")
                        it[endDate] = LocalDate.parse("2026-12-31")
                        it[months] = 12
                        it[monthlyAmount] = BigDecimal("4500.00")
                        it[deposit] = BigDecimal("1350.00")
                        it[totalAmount] = BigDecimal("55350.00")
                        it[status] = ContractStatus.active
                        it[deliveryAddress] = "Praha"
                    }.value
            }

        val service = ContractService(NotificationService())
        assertFailsWith<NotFoundException> {
            service.pdfDocument(ContractId(contractId), "client", 9999L, 1L, JwtService.SCOPE_TENANT)
        }
    }

    @Test
    fun approveNotFoundTest() {
        val service = ContractService(NotificationService())
        assertFailsWith<NotFoundException> {
            service.approve(ContractId(99999))
        }
    }

    @Test
    fun rejectNotFoundTest() {
        val service = ContractService(NotificationService())
        assertFailsWith<NotFoundException> {
            service.reject(ContractId(99999))
        }
    }
}
