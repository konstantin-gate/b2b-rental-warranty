@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Companies
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.TicketStatus
import cz.b2brental.db.Users
import cz.b2brental.db.WarrantyRules
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardServiceTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:dashtest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
                Payments,
                ServiceTickets,
                WarrantyRules,
            )
        }
    }

    @Test
    fun metricsEmptyTest() {
        val paymentService = PaymentService(fixedClock)
        val dashboardService = DashboardService(paymentService)
        val metrics = dashboardService.metrics()
        assertEquals(0, metrics.activeContracts)
        assertEquals(0, metrics.openTickets)
        assertEquals(0, metrics.overduePayments)
    }

    @Test
    fun metricsWithTestDataTest() {
        val compId =
            transaction {
                val cid =
                    Companies.insertAndGetId {
                        it[name] = "Test s.r.o."
                        it[inn] = "28745001"
                        it[address] = "Praha"
                    }
                val catId =
                    EquipmentCategories.insertAndGetId {
                        it[name] = "Chladicí vitrína"
                        it[icon] = "❄️"
                    }
                val eq1 =
                    Equipment.insertAndGetId {
                        it[categoryId] = catId
                        it[model] = "CoolMax 3000"
                        it[serialNumber] = "SN-12345"
                        it[price] = BigDecimal("50000.00")
                        it[monthlyRate] = BigDecimal("4500.00")
                        it[status] = EquipmentStatus.available
                    }
                val eq2 =
                    Equipment.insertAndGetId {
                        it[categoryId] = catId
                        it[model] = "CoolMax 5000"
                        it[serialNumber] = "SN-67890"
                        it[price] = BigDecimal("80000.00")
                        it[monthlyRate] = BigDecimal("7000.00")
                        it[status] = EquipmentStatus.rented
                    }
                val rentalContractId =
                    RentalContracts.insertAndGetId {
                        it[companyId] = cid
                        it[startDate] = LocalDate.parse("2026-01-01")
                        it[endDate] = LocalDate.parse("2026-12-31")
                        it[months] = 12
                        it[monthlyAmount] = BigDecimal("4500.00")
                        it[deposit] = BigDecimal("1350.00")
                        it[totalAmount] = BigDecimal("55350.00")
                        it[status] = ContractStatus.active
                        it[deliveryAddress] = "Praha 5"
                    }
                Payments.insertAndGetId {
                    it[contractId] = rentalContractId.value
                    it[period] = 1
                    it[amount] = BigDecimal("4500.00")
                    it[dueDate] = LocalDate.parse("2026-08-01")
                    it[status] = PaymentStatus.overdue
                }
                ServiceTickets.insertAndGetId {
                    it[equipmentId] = eq1
                    it[companyId] = cid
                    it[description] = "Kompresor hučí"
                    it[severity] = cz.b2brental.db.Severity.medium
                    it[status] = TicketStatus.new
                }
                cid
            }

        val paymentService = PaymentService(fixedClock)
        val dashboardService = DashboardService(paymentService)
        val metrics = dashboardService.metrics()
        assertEquals(1, metrics.activeContracts)
        assertEquals(1, metrics.openTickets)
        assertEquals(1, metrics.overduePayments)
        assertTrue(metrics.overdueAmount.isPositive())
        assertEquals(1, metrics.equipmentByStatus.available)
        assertEquals(1, metrics.equipmentByStatus.rented)
        assertEquals(0, metrics.equipmentByStatus.maintenance)
    }

    @Test
    fun metricsAsContextTest() {
        val paymentService = PaymentService(fixedClock)
        val dashboardService = DashboardService(paymentService)
        val context = dashboardService.metricsAsContext()
        assertTrue(context.contains("Aktivní smlouvy"))
    }
}
