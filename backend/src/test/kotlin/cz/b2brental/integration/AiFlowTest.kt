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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiFlowTest {
    @Test
    fun aiEndpointsAndRoleSecurityFlow(): Unit =
        withB2bTestApp("ai-flow") {
            val clientToken = login("kitchen@b2b.demo", "kitchen123")
            val managerToken = login("manager@b2b.demo", "manager123")
            val techToken = login("tech@b2b.demo", "tech123")

            // 1. Diagnostika poruchy (POST /ai/diagnose) — klient povolen, fallback bez klíče
            val diagResp =
                client.post("/ai/diagnose") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"description\":\"Kompresor hučí a nechladí\"}")
                }
            assertEquals(HttpStatusCode.OK, diagResp.status)
            val diagText = diagResp.bodyAsText()
            assertTrue(diagText.contains("possibleCause"))
            assertTrue(diagText.contains("severity"))

            // 2. Kontrola záruky (POST /ai/warranty-check) — manažer povolen
            val warrantyResp =
                client.post("/ai/warranty-check") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":1,\"description\":\"Závada chlazení\"}")
                }
            assertEquals(HttpStatusCode.OK, warrantyResp.status)
            val warrantyText = warrantyResp.bodyAsText()
            assertTrue(warrantyText.contains("verdict"))
            assertTrue(warrantyText.contains("reason"))

            // 3. AI Asistent (POST /ai/assistant) — manažer povolen, klient zakázán
            val assistantResp =
                client.post("/ai/assistant") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"message\":\"Jaké jsou aktuální tržby?\"}")
                }
            assertEquals(HttpStatusCode.OK, assistantResp.status)
            assertTrue(assistantResp.bodyAsText().contains("reply"))

            val forbiddenAssistant =
                client.post("/ai/assistant") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"message\":\"Data firmy\"}")
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenAssistant.status)

            // 4. Technik nesmí volat /ai/diagnose
            val techDiag =
                client.post("/ai/diagnose") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"description\":\"Zkouška\"}")
                }
            assertEquals(HttpStatusCode.Forbidden, techDiag.status)
        }
}
