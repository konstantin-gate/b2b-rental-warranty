@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardFlowTest {
    @Test
    fun dashboardMetricsAndSecurityFlow(): Unit =
        withB2bTestApp("dash-flow") {
            val managerToken = login("manager@b2b.demo", "manager1234abcd")
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val techToken = login("tech@b2b.demo", "tech12345abcd")

            // 1. Manažer má přístup k metrikám
            val resp =
                client.get("/dashboard") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, resp.status)
            val text = resp.bodyAsText()
            assertTrue(text.contains("activeContracts"))
            assertTrue(text.contains("openTickets"))
            assertTrue(text.contains("overduePayments"))
            assertTrue(text.contains("equipmentByStatus"))
            assertTrue(text.contains("monthStats"))

            // 2. Klient a technik nemají přístup (403)
            val clientResp =
                client.get("/dashboard") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.Forbidden, clientResp.status)

            val techResp =
                client.get("/dashboard") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.Forbidden, techResp.status)

            // 3. Nepřihlášený uživatel (401)
            val unauthResp = client.get("/dashboard")
            assertEquals(HttpStatusCode.Unauthorized, unauthResp.status)
        }
}
