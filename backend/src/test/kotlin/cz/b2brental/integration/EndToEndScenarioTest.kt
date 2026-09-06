@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Black-box E2E test celého scénáře: katalog → smlouva → schválení → PDF → tiket → vyřízení → dashboard
 */
class EndToEndScenarioTest {
    @Test
    fun fullScenarioEndToEndTest(): Unit =
        withB2bTestApp("e2e-scenario") {
            // Krok 1: přihlášení klienta
            val clientToken = login("kitchen@b2b.demo", "kitchen123")

            // Krok 2: čtení katalogu a hledání první dostupné položky
            val catalogResp =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, catalogResp.status)
            val catalogText = catalogResp.bodyAsText()
            val availableItem: String? =
                catalogText
                    .split("{")
                    .firstOrNull { item -> item.contains("\"status\":\"available\"") }
            assertTrue(availableItem != null, "Katalog nemá žádnou dostupnou položku")
            val equipmentId: Long =
                Regex("\"id\":(\\d+)")
                    .find(availableItem)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nezdařilo se přečíst id dostupné položky katalogu")

            // Krok 3: vytvoření nájemní smlouvy klientem
            val createContractResp =
                client.post("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"equipmentIds\":[$equipmentId],\"months\":6," +
                            "\"startDate\":\"${LocalDate.now()}\",\"deliveryAddress\":\"Praha E2E\"}",
                    )
                }
            assertEquals(HttpStatusCode.Created, createContractResp.status)
            val contractText = createContractResp.bodyAsText()
            assertTrue(contractText.contains("\"status\":\"draft\""))
            val contractId: Long =
                Regex("\"id\":(\\d+)")
                    .find(contractText)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nezdařilo se přečíst id smlouvy z odpovědi")

            // Krok 4: přihlášení manažera
            val managerToken = login("manager@b2b.demo", "manager123")

            // Krok 5: schválení smlouvy manažerem
            val approveResp =
                client.post("/contracts/$contractId/approve") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, approveResp.status)
            assertTrue(approveResp.bodyAsText().contains("\"status\":\"active\""))

            // Krok 6: vygenerování PDF smlouvy
            val pdfResp =
                client.post("/contracts/$contractId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, pdfResp.status)
            val documentId: Long =
                Regex("\"documentId\":(\\d+)")
                    .find(pdfResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nezdařilo se přečíst documentId z odpovědi")

            // Krok 7: stažení PDF dokumentu
            val downloadResp =
                client.get("/documents/$documentId/pdf") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, downloadResp.status)
            assertTrue(downloadResp.bodyAsText().startsWith("%PDF"))

            // Krok 8: opětovné přihlášení klienta a vytvoření tiketu na pronajaté vybavení
            val clientTokenAgain = login("kitchen@b2b.demo", "kitchen123")
            val createTicketResp =
                client.post("/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $clientTokenAgain")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":$equipmentId,\"description\":\"Kompresor hučí a nechladí po instalaci\"}")
                }
            assertEquals(HttpStatusCode.Created, createTicketResp.status)
            val ticketText = createTicketResp.bodyAsText()
            assertTrue(ticketText.contains("severity"))
            assertTrue(ticketText.contains("warrantyVerdict"))
            assertTrue(ticketText.contains("aiRecommendation"))
            assertTrue(ticketText.contains("\"status\":\"new\""))
            val ticketId: Long =
                Regex("\"id\":(\\d+)")
                    .find(ticketText)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nezdařilo se přečíst id tiketu z odpovědi")

            // Krok 9: přiřazení technika, zahájení a vyřešení tiketu
            val usersResp =
                client.get("/users?role=technician") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, usersResp.status)
            val techId: Long =
                Regex("\"id\":(\\d+)")
                    .find(usersResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nezdařilo se přečíst id technika z odpovědi")

            val assignResp =
                client.post("/tickets/$ticketId/assign") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"technicianId\":$techId}")
                }
            assertEquals(HttpStatusCode.OK, assignResp.status)
            assertTrue(assignResp.bodyAsText().contains("\"status\":\"assigned\""))

            val techToken = login("tech@b2b.demo", "tech123")

            val startResp =
                client.post("/tickets/$ticketId/start") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.OK, startResp.status)
            assertTrue(startResp.bodyAsText().contains("\"status\":\"in_progress\""))

            val resolveResp =
                client.post("/tickets/$ticketId/resolve") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"result\":\"repaired\",\"notes\":\"Opraveno v E2E\"}")
                }
            assertEquals(HttpStatusCode.OK, resolveResp.status)
            assertTrue(resolveResp.bodyAsText().contains("\"status\":\"resolved\""))

            // Krok 10: ověření vyřešeného tiketu klientem a dashboardu manažerem
            val clientTokenThird = login("kitchen@b2b.demo", "kitchen123")
            val ticketDetailResp =
                client.get("/tickets/$ticketId") {
                    header(HttpHeaders.Authorization, "Bearer $clientTokenThird")
                }
            assertEquals(HttpStatusCode.OK, ticketDetailResp.status)
            assertTrue(ticketDetailResp.bodyAsText().contains("\"status\":\"resolved\""))

            val managerTokenAgain = login("manager@b2b.demo", "manager123")
            val dashboardResp =
                client.get("/dashboard") {
                    header(HttpHeaders.Authorization, "Bearer $managerTokenAgain")
                }
            assertEquals(HttpStatusCode.OK, dashboardResp.status)
            val dashboardText = dashboardResp.bodyAsText()
            assertTrue(dashboardText.contains("\"openTickets\""))
            assertTrue(dashboardText.contains("\"monthStats\""))
        }
}
