@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
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
 * Black-box testy krajních případů: 409 bez aktivní smlouvy, 409 pro již pronajaté
 * vybavení a AI fallback s vypnutou AI
 */
class EdgeCasesTest {
    @Test
    fun createTicketWithoutContractGivesConflictTest(): Unit =
        withB2bTestApp("e2e-edge-1") {
            // Přihlášení klienta a pokus o tiket na vybavení 5 (mimo aktivní smlouvu firmy)
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val resp =
                client.post("/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":5,\"description\":\"Vitrína nechladí vůbec\"}")
                }
            assertEquals(HttpStatusCode.Conflict, resp.status)
            assertTrue(resp.bodyAsText().contains("\"code\":\"CONFLICT\""))
        }

    @Test
    fun warrantyCheckManagerVerdictTest(): Unit =
        withB2bTestApp("e2e-edge-2") {
            // Kontrola záruky manažerem pro vybavení 1 — vyloučené příčiny již neovlivňují verdikt (R3 odstraněno)
            val managerToken = login("manager@b2b.demo", "manager1234abcd")
            val resp =
                client.post("/ai/warranty-check") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":1,\"description\":\"Závada kvůli opotřebení kompresoru\"}")
                }
            assertEquals(HttpStatusCode.OK, resp.status)
            val text = resp.bodyAsText()
            assertTrue(text.contains("\"verdict\":\"covered\""))
            assertTrue(text.contains("\"reason\""))
        }

    @Test
    fun createContractForRentedEquipmentGivesConflictTest(): Unit =
        withB2bTestApp("e2e-edge-3") {
            // Pokus o smlouvu na již pronajaté vybavení 1 → konflikt
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val resp =
                client.post("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"equipmentIds\":[1],\"months\":6," +
                            "\"startDate\":\"${LocalDate.now()}\",\"deliveryAddress\":\"Praha Edge\"}",
                    )
                }
            assertEquals(HttpStatusCode.Conflict, resp.status)
            assertTrue(resp.bodyAsText().contains("\"code\":\"CONFLICT\""))
        }

    @Test
    fun diagnoseFallbackWhenAiDisabledTest(): Unit =
        withB2bTestApp("e2e-edge-4") {
            // Diagnostika s vypnutou AI → 200 a fallback s hlášením o nedostupnosti AI
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val resp =
                client.post("/ai/diagnose") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"description\":\"Kompresor hučí a nechladí\"}")
                }
            assertEquals(HttpStatusCode.OK, resp.status)
            assertTrue(resp.bodyAsText().contains("AI není dostupná"))
        }
}
