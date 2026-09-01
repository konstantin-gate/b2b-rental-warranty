@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Companies
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.HistoryEvents
import cz.b2brental.db.Notifications
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.TicketStatus
import cz.b2brental.db.Users
import cz.b2brental.db.WarrantyRules
import cz.b2brental.utils.NotFoundException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PdfServiceTest {
    private lateinit var pdfService: PdfService

    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:pdftest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
                HistoryEvents,
                Documents,
                Notifications,
            )
        }
        pdfService = PdfService()
    }

    @Test
    fun rentalContractPdfTest() {
        val contractId =
            transaction {
                val catId =
                    EquipmentCategories.insertAndGetId {
                        it[name] = "Chladicí vitrína"
                        it[icon] = "❄️"
                    }
                val eqId =
                    Equipment.insertAndGetId {
                        it[categoryId] = catId
                        it[model] = "CoolMax 3000"
                        it[serialNumber] = "SN-12345"
                        it[price] = BigDecimal("50000.00")
                        it[monthlyRate] = BigDecimal("4500.00")
                        it[status] = EquipmentStatus.available
                    }
                val compId =
                    Companies.insertAndGetId {
                        it[name] = "Test s.r.o."
                        it[inn] = "28745001"
                        it[address] = "Praha 1"
                    }
                val cId =
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
                            it[deliveryAddress] = "Praha 5, Revoluční 1082/8"
                        }.value
                ContractItems.insertAndGetId {
                    it[contractId] = cId
                    it[equipmentId] = eqId.value
                }
                cId
            }

        val bytes = pdfService.render(DocumentType.rental_contract, contractId)
        assertNotNull(bytes)
        assertTrue(bytes.size > 100)
        val header = String(bytes, 0, minOf(5, bytes.size))
        assertTrue(header.startsWith("%PDF"))
    }

    @Test
    fun acceptanceActPdfTest() {
        val equipmentId =
            transaction {
                val catId =
                    EquipmentCategories.insertAndGetId {
                        it[name] = "Chladicí vitrína"
                        it[icon] = "❄️"
                    }
                Equipment
                    .insertAndGetId {
                        it[categoryId] = catId
                        it[model] = "CoolMax 3000"
                        it[serialNumber] = "SN-12345"
                        it[price] = BigDecimal("50000.00")
                        it[monthlyRate] = BigDecimal("4500.00")
                        it[status] = EquipmentStatus.available
                    }.value
            }

        val bytes = pdfService.render(DocumentType.acceptance_act, equipmentId)
        assertNotNull(bytes)
        assertTrue(bytes.size > 100)
    }

    @Test
    fun returnActPdfTest() {
        val equipmentId =
            transaction {
                val catId =
                    EquipmentCategories.insertAndGetId {
                        it[name] = "Chladicí vitrína"
                        it[icon] = "❄️"
                    }
                Equipment
                    .insertAndGetId {
                        it[categoryId] = catId
                        it[model] = "CoolMax 3000"
                        it[serialNumber] = "SN-12345"
                        it[price] = BigDecimal("50000.00")
                        it[monthlyRate] = BigDecimal("4500.00")
                        it[status] = EquipmentStatus.rented
                    }.value
            }

        val bytes = pdfService.render(DocumentType.return_act, equipmentId)
        assertNotNull(bytes)
        assertTrue(bytes.size > 100)
    }

    @Test
    fun serviceReportPdfTest() {
        val ticketId =
            transaction {
                val catId =
                    EquipmentCategories.insertAndGetId {
                        it[name] = "Chladicí vitrína"
                        it[icon] = "❄️"
                    }
                val eqId =
                    Equipment.insertAndGetId {
                        it[categoryId] = catId
                        it[model] = "CoolMax 3000"
                        it[serialNumber] = "SN-12345"
                        it[price] = BigDecimal("50000.00")
                        it[monthlyRate] = BigDecimal("4500.00")
                        it[status] = EquipmentStatus.rented
                    }
                val compId =
                    Companies.insertAndGetId {
                        it[name] = "Test s.r.o."
                        it[inn] = "28745001"
                        it[address] = "Praha 1"
                    }
                val userId =
                    Users.insertAndGetId {
                        it[companyId] = compId
                        it[email] = "tech@b2b.demo"
                        it[passwordHash] = "hash"
                        it[role] = "technician"
                        it[phone] = "+420123456789"
                    }
                ServiceTickets
                    .insertAndGetId {
                        it[equipmentId] = eqId
                        it[companyId] = compId
                        it[description] = "Kompresor hučí"
                        it[severity] = cz.b2brental.db.Severity.medium
                        it[warrantyVerdict] = cz.b2brental.db.WarrantyVerdict.covered
                        it[aiRecommendation] = "Vyměnit ventilátor"
                        it[status] = TicketStatus.resolved
                        it[resolution] = "repaired"
                        it[technicianId] = userId.value
                        it[resolvedAt] = Instant.now()
                    }.value
            }

        val bytes = pdfService.render(DocumentType.service_report, ticketId)
        assertNotNull(bytes)
        assertTrue(bytes.size > 100)
    }

    @Test
    fun invoicePdfTest() {
        val paymentId =
            transaction {
                val compId =
                    Companies.insertAndGetId {
                        it[name] = "Test s.r.o."
                        it[inn] = "28745001"
                        it[address] = "Praha 1"
                    }
                val rentalContractId =
                    RentalContracts.insertAndGetId {
                        it[companyId] = compId
                        it[startDate] = LocalDate.parse("2026-01-01")
                        it[endDate] = LocalDate.parse("2026-12-31")
                        it[months] = 12
                        it[monthlyAmount] = BigDecimal("4500.00")
                        it[deposit] = BigDecimal("1350.00")
                        it[totalAmount] = BigDecimal("55350.00")
                        it[status] = ContractStatus.active
                        it[deliveryAddress] = "Praha 5"
                    }
                Payments
                    .insertAndGetId {
                        it[contractId] = rentalContractId.value
                        it[period] = 1
                        it[amount] = BigDecimal("4500.00")
                        it[dueDate] = LocalDate.parse("2026-02-01")
                        it[status] = PaymentStatus.paid
                    }.value
            }

        val bytes = pdfService.render(DocumentType.invoice, paymentId)
        assertNotNull(bytes)
        assertTrue(bytes.size > 100)
    }

    @Test
    fun rentalContractNotFoundTest() {
        assertFailsWith<NotFoundException> {
            pdfService.render(DocumentType.rental_contract, 99999L)
        }
    }
}
