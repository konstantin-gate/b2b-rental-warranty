@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental.integration

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import cz.b2brental.auth.JwtService
import cz.b2brental.db.Companies
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.Severity
import cz.b2brental.db.TicketStatus
import cz.b2brental.db.Users
import cz.b2brental.module
import cz.b2brental.utils.login
import cz.b2brental.utils.testConfig
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.net.Socket
import java.time.LocalDate
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Registrace firmy s tenant adminem; vrací token admina a id jeho firmy. */
private suspend fun ApplicationTestBuilder.registerTenantAdminWithCompany(email: String): Pair<String, Long> {
    val regResp =
        client.post("/auth/register-company") {
            contentType(ContentType.Application.Json)
            setBody(
                "{\"companyName\":\"Tenant Sec s.r.o.\",\"inn\":\"28745019\"," +
                    "\"address\":\"Ostrava\",\"adminEmail\":\"$email\"," +
                    "\"password\":\"secret1234abcd\"}",
            )
        }
    assertEquals(HttpStatusCode.Created, regResp.status)
    val token: String = login(email, "secret1234abcd")
    val companyId: Long =
        transaction {
            Users
                .selectAll()
                .where { Users.email eq email }
                .first()[Users.companyId]!!
                .value
        }
    return token to companyId
}

/** Registrace firmy s tenant adminem; vrací token admina po přihlášení. */
private suspend fun ApplicationTestBuilder.registerTenantAdmin(email: String): String = registerTenantAdminWithCompany(email).first

/** Vrací id platformového technika (bez firmy). */
private fun platformTechnicianId(): Long =
    transaction {
        Users
            .selectAll()
            .where { (Users.role eq "technician") and Users.companyId.isNull() }
            .first()[Users.id]
            .value
    }

/** Vrací id libovolné smlouvy v testovací databázi. */
private fun anyContractId(): Long =
    transaction {
        RentalContracts.selectAll().first()[RentalContracts.id].value
    }

/** Vrací id libovolné kategorie vybavení. */
private fun anyCategoryId(): Long =
    transaction {
        EquipmentCategories.selectAll().first()[EquipmentCategories.id].value
    }

/** Vrací id vybavení, které je součástí aktivní smlouvy. */
private fun equipmentIdInActiveContract(): Long =
    transaction {
        (ContractItems innerJoin RentalContracts)
            .selectAll()
            .where { RentalContracts.status eq ContractStatus.active }
            .first()[ContractItems.equipmentId]
            .value
    }

/** Vrací id vybavení, které technik nemá v žádném tiketu. */
private fun foreignEquipmentId(technicianId: Long): Long =
    transaction {
        val ownEquipmentIds: Set<Long> =
            ServiceTickets
                .selectAll()
                .where { ServiceTickets.technicianId eq technicianId }
                .map { row -> row[ServiceTickets.equipmentId].value }
                .toSet()
        Equipment
            .selectAll()
            .map { row -> row[Equipment.id].value }
            .first { equipmentId -> equipmentId !in ownEquipmentIds }
    }

/** Vrací id nezaplacené platby se splatností v minulosti. */
private fun overduePaymentId(): Long =
    transaction {
        val today: LocalDate = LocalDate.now()
        Payments
            .selectAll()
            .where { (Payments.status eq PaymentStatus.unpaid) and (Payments.dueDate less today) }
            .first()[Payments.id]
            .value
    }

/** Vrací id nezaplacené platby se splatností v budoucnosti. */
private fun futurePaymentId(): Long =
    transaction {
        val today: LocalDate = LocalDate.now()
        Payments
            .selectAll()
            .where { (Payments.status eq PaymentStatus.unpaid) and (Payments.dueDate greater today) }
            .first()[Payments.id]
            .value
    }

class SecurityHardeningTest {
    @Test
    fun tokenLogoutRevokesItTest(): Unit =
        withB2bTestApp("sec-logout") {
            val token = login("admin@b2b.demo", "admin1234abcd")
            val logoutResp =
                client.post("/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.OK, logoutResp.status)
            val poOdhlaseni =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.Unauthorized, poOdhlaseni.status)
            val logoutBezTokenu = client.post("/auth/logout")
            assertEquals(HttpStatusCode.Unauthorized, logoutBezTokenu.status)
        }

    @Test
    fun tenantAdminRejectsForeignContractTest(): Unit =
        withB2bTestApp("sec-tenant-approve") {
            val tenantToken = registerTenantAdmin("tenantapprove@b2b.demo")
            val contractId: Long = anyContractId()
            val approveResp =
                client.post("/contracts/$contractId/approve") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.Forbidden, approveResp.status)
            val rejectResp =
                client.post("/contracts/$contractId/reject") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.Forbidden, rejectResp.status)
        }

    @Test
    fun tenantAdminSeesOnlyOwnDocumentsTest(): Unit =
        withB2bTestApp("sec-tenant-docs") {
            val adminToken = login("admin@b2b.demo", "admin1234abcd")
            val platformContractId: Long = anyContractId()
            val docResp =
                client.post("/contracts/$platformContractId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.OK, docResp.status)
            val tenantToken = registerTenantAdmin("tenantdocs@b2b.demo")
            val listResp =
                client.get("/documents") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.OK, listResp.status)
            assertEquals("[]", listResp.bodyAsText())
            val docId: String =
                Regex("\"documentId\":(\\d+)").find(docResp.bodyAsText())?.groupValues?.get(1)
                    ?: error("Chybí documentId v odpovědi")
            val pdfResp =
                client.get("/documents/$docId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.NotFound, pdfResp.status)
        }

    @Test
    fun technicianWarrantyCheckOnlyOwnEquipmentTest(): Unit =
        withB2bTestApp("sec-tech-warranty") {
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val managerToken = login("manager@b2b.demo", "manager1234abcd")
            val techToken = login("tech@b2b.demo", "tech12345abcd")
            val technicianId: Long = platformTechnicianId()
            val ownEquipmentId: Long = equipmentIdInActiveContract()
            val ticketResp =
                client.post("/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":$ownEquipmentId,\"description\":\"Kompresor hučí a nechladí\"}")
                }
            assertEquals(HttpStatusCode.Created, ticketResp.status)
            val ticketId: Long =
                Regex("\"id\":(\\d+)")
                    .find(ticketResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Chybí id tiketu v odpovědi")
            val assignResp =
                client.post("/tickets/$ticketId/assign") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"technicianId\":$technicianId}")
                }
            assertEquals(HttpStatusCode.OK, assignResp.status)
            val ownResp =
                client.post("/ai/warranty-check") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":$ownEquipmentId,\"description\":\"Závada chlazení\"}")
                }
            assertEquals(HttpStatusCode.OK, ownResp.status)
            assertTrue(ownResp.bodyAsText().contains("\"verdict\""))
            val foreignEqId: Long = foreignEquipmentId(technicianId)
            val foreignResp =
                client.post("/ai/warranty-check") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":$foreignEqId,\"description\":\"Závada chlazení\"}")
                }
            assertEquals(HttpStatusCode.NotFound, foreignResp.status)
        }

    @Test
    fun duplicateIcoReturnsConflictTest(): Unit =
        withB2bTestApp("sec-inn-dup") {
            val regResp =
                client.post("/auth/register-company") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"companyName\":\"Dupl Ico s.r.o.\",\"inn\":\"28745001\"," +
                            "\"address\":\"Pardubice\",\"adminEmail\":\"dupico@b2b.demo\"," +
                            "\"password\":\"secret1234abcd\"}",
                    )
                }
            assertEquals(HttpStatusCode.Conflict, regResp.status)
        }

    @Test
    fun clientCannotPayBeforeDueDateTest(): Unit =
        withB2bTestApp("sec-early-pay") {
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val overdueId: Long = overduePaymentId()
            val futureId: Long = futurePaymentId()
            // Platba se splatností v minulosti (seed) → povolena
            val overdueResp =
                client.post("/payments/$overdueId/pay") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, overdueResp.status)
            // Platba se splatností v budoucnosti (seed) → 409
            val earlyResp =
                client.post("/payments/$futureId/pay") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.Conflict, earlyResp.status)
        }

    @Test
    fun tokenWithoutScopeOrNonNumericSubIsRejectedTest(): Unit =
        withB2bTestApp("sec-crafted-token") {
            val secret = "test-secret-32-znaku-minimum-pro-hs256"
            val nowMs = System.currentTimeMillis()
            val noScopeToken =
                JWT
                    .create()
                    .withIssuer(JwtService.ISSUER)
                    .withAudience(JwtService.AUDIENCE)
                    .withExpiresAt(Date(nowMs + 3_600_000L))
                    .withSubject("1")
                    .withClaim(JwtService.CLAIM_ROLE, "admin")
                    .sign(Algorithm.HMAC256(secret))
            val respNoScope =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $noScopeToken")
                }
            assertEquals(HttpStatusCode.Unauthorized, respNoScope.status)
            val badSubToken =
                JWT
                    .create()
                    .withIssuer(JwtService.ISSUER)
                    .withAudience(JwtService.AUDIENCE)
                    .withExpiresAt(Date(nowMs + 3_600_000L))
                    .withSubject("abc")
                    .withClaim(JwtService.CLAIM_ROLE, "admin")
                    .withClaim(JwtService.CLAIM_SCOPE, JwtService.SCOPE_PLATFORM)
                    .sign(Algorithm.HMAC256(secret))
            val respBadSub =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $badSubToken")
                }
            assertEquals(HttpStatusCode.Unauthorized, respBadSub.status)
        }

    @Test
    fun tokenWithoutExpirationIsRejectedTest(): Unit =
        withB2bTestApp("sec-no-exp") {
            val token =
                JWT
                    .create()
                    .withIssuer(JwtService.ISSUER)
                    .withAudience(JwtService.AUDIENCE)
                    .withSubject("1")
                    .withClaim(JwtService.CLAIM_ROLE, "admin")
                    .withClaim(JwtService.CLAIM_SCOPE, JwtService.SCOPE_PLATFORM)
                    .sign(Algorithm.HMAC256("test-secret-32-znaku-minimum-pro-hs256"))
            val resp =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.Unauthorized, resp.status)
        }

    @Test
    fun usersMaskedForTenantAdminTest(): Unit =
        withB2bTestApp("sec-users-masked") {
            val tenantToken = registerTenantAdmin("tenantusers@b2b.demo")
            val resp =
                client.get("/users?role=technician") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.OK, resp.status)
            val text = resp.bodyAsText()
            assertTrue(text.contains("***"))
            assertTrue(!text.contains("tech@b2b.demo"))
        }

    @Test
    fun rentedEquipmentCannotBeModifiedTest(): Unit =
        withB2bTestApp("sec-catalog-rented") {
            val adminToken = login("admin@b2b.demo", "admin1234abcd")
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val categoryId: Long = anyCategoryId()
            val body =
                "{\"categoryId\":$categoryId,\"model\":\"Test Model Sec\",\"serialNumber\":\"SN-SEC-001\"," +
                    "\"price\":\"100000.00\",\"monthlyRate\":\"2500.00\",\"description\":\"Test\"}"
            val createResp =
                client.post("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            assertEquals(HttpStatusCode.Created, createResp.status)
            val eqId: String =
                Regex("\"id\":(\\d+)").find(createResp.bodyAsText())?.groupValues?.get(1)
                    ?: error("Chybí id vybavení v odpovědi")
            val updateResp =
                client.put("/catalog/$eqId") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            assertEquals(HttpStatusCode.OK, updateResp.status)
            val contractResp =
                client.post("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"equipmentIds\":[$eqId],\"months\":1," +
                            "\"startDate\":\"${LocalDate.now()}\",\"deliveryAddress\":\"Praha Sec\"}",
                    )
                }
            assertEquals(HttpStatusCode.Created, contractResp.status)
            val contractId: String =
                Regex("\"id\":(\\d+)").find(contractResp.bodyAsText())?.groupValues?.get(1)
                    ?: error("Chybí id smlouvy v odpovědi")
            val approveResp =
                client.post("/contracts/$contractId/approve") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.OK, approveResp.status)
            val updateRentedResp =
                client.put("/catalog/$eqId") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            assertEquals(HttpStatusCode.Conflict, updateRentedResp.status)
        }

    @Test
    fun tenantWithoutContractsSeesEmptyPaymentsTest(): Unit =
        withB2bTestApp("sec-tenant-payments") {
            val tenantToken = registerTenantAdmin("tenantpayments@b2b.demo")
            val paymentsResp =
                client.get("/payments") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.OK, paymentsResp.status)
            assertEquals("[]", paymentsResp.bodyAsText())
            val ticketsResp =
                client.get("/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals("[]", ticketsResp.bodyAsText())
        }

    @Test
    fun chunkedBodyIsRejectedTest(): Unit =
        runBlocking {
            // Reálný server Netty na náhodném portu: Ktor test client blokuje hlavičku
            // Transfer-Encoding, proto se požadavek posílá přes raw socket.
            TransactionManager.resetCurrent(null)
            val server =
                embeddedServer(Netty, port = 0) {
                    module(testConfig("sec-chunked-raw"))
                }.start(wait = false)
            try {
                val port: Int =
                    server
                        .engine
                        .resolvedConnectors()
                        .first()
                        .port
                val body = "{\"email\":\"x\",\"password\":\"y\"}"
                val request: String =
                    "POST /auth/login HTTP/1.1\r\n" +
                        "Host: 127.0.0.1\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        body.length.toString(16) + "\r\n" +
                        body + "\r\n" +
                        "0\r\n" +
                        "\r\n"
                val statusLine: String =
                    Socket("127.0.0.1", port).use { socket ->
                        socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                        socket.getOutputStream().flush()
                        socket.getInputStream().bufferedReader().readLine() ?: ""
                    }
                assertTrue(statusLine.contains("413"), "Očekáván status 413, přijato: $statusLine")
            } finally {
                server.stop(0, 2_000)
            }
        }

    @Test
    fun largeBodyIsRejectedTest(): Unit =
        withB2bTestApp("sec-big-body") {
            val resp =
                client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("a".repeat(8_388_609))
                }
            assertEquals(HttpStatusCode.PayloadTooLarge, resp.status)
        }

    @Test
    fun tenantAssigningForeignTechnicianIsRejectedTest(): Unit =
        withB2bTestApp("sec-assign-tenant") {
            val (tenantToken, tenantCompanyId) = registerTenantAdminWithCompany("tenantassign@b2b.demo")
            val foreignTechnicianId: Long =
                transaction {
                    Users
                        .insertAndGetId {
                            it[role] = "technician"
                            it[email] = "foreign-tech@b2b.demo"
                            it[passwordHash] = BCrypt.hashpw("foreign1234abcd", BCrypt.gensalt())
                            it[Users.companyId] = EntityID(1L, Companies)
                        }.value
                }
            val equipmentId: Long = equipmentIdInActiveContract()
            val ticketId: Long =
                transaction {
                    ServiceTickets
                        .insertAndGetId {
                            it[ServiceTickets.companyId] = EntityID(tenantCompanyId, Companies)
                            it[ServiceTickets.equipmentId] = EntityID(equipmentId, Equipment)
                            it[description] = "Závada chlazení"
                            it[severity] = Severity.medium
                            it[status] = TicketStatus.new
                        }.value
                }
            val foreignAssign =
                client.post("/tickets/$ticketId/assign") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"technicianId\":$foreignTechnicianId}")
                }
            assertEquals(HttpStatusCode.NotFound, foreignAssign.status)
            val platformAssign =
                client.post("/tickets/$ticketId/assign") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"technicianId\":${platformTechnicianId()}}")
                }
            assertEquals(HttpStatusCode.OK, platformAssign.status)
        }

    @Test
    fun tenantSeesOwnDocumentTest(): Unit =
        withB2bTestApp("sec-tenant-docs-own") {
            val adminToken = login("admin@b2b.demo", "admin1234abcd")
            val platformContractId: Long = anyContractId()
            val platformDocResp =
                client.post("/contracts/$platformContractId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.OK, platformDocResp.status)

            val (tenantToken, tenantCompanyId) = registerTenantAdminWithCompany("tenantdocsown@b2b.demo")
            val contractId: Long =
                transaction {
                    RentalContracts
                        .insertAndGetId {
                            it[companyId] = EntityID(tenantCompanyId, Companies)
                            it[startDate] = LocalDate.now().minusDays(10)
                            it[endDate] = LocalDate.now().plusMonths(1)
                            it[months] = 1
                            it[monthlyAmount] = BigDecimal("1000.00")
                            it[deposit] = BigDecimal("0.00")
                            it[totalAmount] = BigDecimal("1000.00")
                            it[status] = ContractStatus.active
                            it[deliveryAddress] = "Ostrava"
                        }.value
                }
            val ownDocResp =
                client.post("/contracts/$contractId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.OK, ownDocResp.status)

            val listResp =
                client.get("/documents") {
                    header(HttpHeaders.Authorization, "Bearer $tenantToken")
                }
            assertEquals(HttpStatusCode.OK, listResp.status)
            val listText: String = listResp.bodyAsText()
            assertTrue(listText.contains("\"entityId\":$contractId"))
            assertTrue(!listText.contains("\"entityId\":$platformContractId,"))
        }
}
