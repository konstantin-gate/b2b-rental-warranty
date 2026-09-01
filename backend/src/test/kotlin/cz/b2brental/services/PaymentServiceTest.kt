@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Companies
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.Users
import cz.b2brental.domain.PaymentId
import cz.b2brental.utils.ConflictException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PaymentServiceTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC)
    private lateinit var service: PaymentService
    private var contractId: Long = 0L

    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:paytest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction {
            SchemaUtils.create(Companies, Users, RentalContracts, Payments)
            val cId =
                Companies.insertAndGetId {
                    it[name] = "Test s.r.o."
                    it[inn] = "28745001"
                    it[address] = "Praha"
                }
            contractId =
                RentalContracts
                    .insertAndGetId {
                        it[companyId] = cId
                        it[startDate] = LocalDate.parse("2026-01-01")
                        it[endDate] = LocalDate.parse("2026-12-31")
                        it[months] = 12
                        it[monthlyAmount] = BigDecimal("9000.00")
                        it[deposit] = BigDecimal("2700.00")
                        it[totalAmount] = BigDecimal("110700.00")
                        it[status] = ContractStatus.active
                        it[deliveryAddress] = "Praha"
                    }.value
        }
        service = PaymentService(fixedClock)
    }

    @Test
    fun refreshOverdueAndPayTest() {
        var overduePaymentId = 0L
        var futurePaymentId = 0L

        transaction {
            overduePaymentId =
                Payments
                    .insertAndGetId {
                        it[contractId] = EntityID(this@PaymentServiceTest.contractId, RentalContracts)
                        it[period] = 1
                        it[amount] = BigDecimal("9000.00")
                        it[dueDate] = LocalDate.parse("2026-08-01")
                        it[status] = PaymentStatus.unpaid
                    }.value

            futurePaymentId =
                Payments
                    .insertAndGetId {
                        it[contractId] = EntityID(this@PaymentServiceTest.contractId, RentalContracts)
                        it[period] = 2
                        it[amount] = BigDecimal("9000.00")
                        it[dueDate] = LocalDate.parse("2026-09-15")
                        it[status] = PaymentStatus.unpaid
                    }.value
        }

        service.refreshOverdue()

        transaction {
            val p1 = Payments.selectAll().where { Payments.id eq overduePaymentId }.single()
            assertEquals(PaymentStatus.overdue, p1[Payments.status])

            val p2 = Payments.selectAll().where { Payments.id eq futurePaymentId }.single()
            assertEquals(PaymentStatus.unpaid, p2[Payments.status])
        }

        val actionRes = service.pay(PaymentId(overduePaymentId), "manager", null)
        assertEquals(PaymentStatus.paid, actionRes.status)
        assertNotNull(actionRes.paidAt)

        assertFailsWith<ConflictException> {
            service.pay(PaymentId(overduePaymentId), "manager", null)
        }
    }
}
