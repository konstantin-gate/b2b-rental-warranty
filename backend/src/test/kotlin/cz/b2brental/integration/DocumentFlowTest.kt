@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.db.Companies
import cz.b2brental.db.Users
import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentFlowTest {
    @Test
    fun documentLifecycleAndDownloadFlow(): Unit =
        withB2bTestApp("doc-flow") {
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val managerToken = login("manager@b2b.demo", "manager1234abcd")
            val techToken = login("tech@b2b.demo", "tech12345abcd")

            // Druhá firma a její klient pro test izolace přístupu (přímý zápis do testovací DB)
            transaction {
                val secondCompanyId =
                    Companies.insertAndGetId {
                        it[name] = "Druha Firma s.r.o."
                        it[inn] = "25596641"
                        it[address] = "Brno"
                    }
                Users.insertAndGetId {
                    it[role] = "client"
                    it[email] = "druha@b2b.demo"
                    it[passwordHash] = BCrypt.hashpw("druha123", BCrypt.gensalt())
                    it[Users.companyId] = secondCompanyId
                }
            }
            val otherClientToken = login("druha@b2b.demo", "druha123")

            // 1. Vygenerování PDF smlouvy (POST /contracts/1/pdf)
            val contractPdfResp =
                client.post("/contracts/1/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, contractPdfResp.status)
            val contractDocId =
                Regex("\"documentId\":(\\d+)")
                    .find(contractPdfResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong() ?: return@withB2bTestApp

            // 2. Idempotence: opakované volání vrací stejné documentId
            val contractPdfResp2 =
                client.post("/contracts/1/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, contractPdfResp2.status)
            val contractDocId2 =
                Regex("\"documentId\":(\\d+)")
                    .find(contractPdfResp2.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong() ?: return@withB2bTestApp
            assertEquals(contractDocId, contractDocId2)

            // 3. Vygenerování akceptačního protokolu (POST /contracts/1/acceptance-act)
            val acceptResp =
                client.post("/contracts/1/acceptance-act") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, acceptResp.status)

            // 4. Vygenerování protokolu o vrácení (POST /contracts/1/return-act)
            val returnResp =
                client.post("/contracts/1/return-act") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, returnResp.status)

            // 5. Vygenerování faktury (POST /payments/1/pdf)
            val invoiceResp =
                client.post("/payments/1/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, invoiceResp.status)
            val invoiceDocId =
                Regex("\"documentId\":(\\d+)")
                    .find(invoiceResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong() ?: return@withB2bTestApp

            // 6. Vygenerování servisní zprávy k vyřešenému tiketu 3 (POST /tickets/3/pdf)
            val ticketDocResp =
                client.post("/tickets/3/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, ticketDocResp.status)

            // 7. Seznam dokumentů (GET /documents)
            val listResp =
                client.get("/documents") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, listResp.status)
            val listText = listResp.bodyAsText()
            assertTrue(listText.contains("rental_contract"))
            assertTrue(listText.contains("acceptance_act"))
            assertTrue(listText.contains("return_act"))
            assertTrue(listText.contains("invoice"))
            assertTrue(listText.contains("service_report"))

            // 8. Stažení binárního PDF (GET /documents/{id}/pdf)
            val pdfDownloadResp =
                client.get("/documents/$contractDocId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, pdfDownloadResp.status)
            assertEquals("application/pdf", pdfDownloadResp.headers[HttpHeaders.ContentType])
            assertEquals(true, pdfDownloadResp.headers[HttpHeaders.ContentDisposition]?.contains("rental_contract-"))
            val bytes = pdfDownloadResp.bodyAsBytes()
            assertTrue(bytes.size > 200)
            assertEquals("%PDF-", String(bytes, 0, 5))

            // 9. Izolace: klient druhé firmy nemá přístup (404)
            val otherClientForbidden =
                client.get("/documents/$contractDocId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $otherClientToken")
                }
            assertEquals(HttpStatusCode.NotFound, otherClientForbidden.status)

            // 10. Technik nesmí stáhnout smlouvu (404)
            val techForbiddenContract =
                client.get("/documents/$contractDocId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.NotFound, techForbiddenContract.status)

            // 11. Technik nesmí stáhnout fakturu (404)
            val techForbiddenInvoice =
                client.get("/documents/$invoiceDocId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.NotFound, techForbiddenInvoice.status)

            // 12. Neautorizovaný přístup (401)
            val unauthResp = client.get("/documents/$contractDocId/pdf")
            assertEquals(HttpStatusCode.Unauthorized, unauthResp.status)

            // 13. Neexistující dokument (404)
            val notFoundResp =
                client.get("/documents/99999/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.NotFound, notFoundResp.status)
        }
}
