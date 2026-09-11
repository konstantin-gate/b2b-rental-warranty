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

class HealthFlowTest {
    @Test
    fun healthEndpointTest(): Unit =
        withB2bTestApp("health-flow") {
            val resp = client.get("/health")
            assertEquals(HttpStatusCode.OK, resp.status)
            assertTrue(resp.bodyAsText().contains("ok"))
        }

    @Test
    fun dashboardRequiresAuthTest(): Unit =
        withB2bTestApp("dashboard-auth-flow") {
            val resp = client.get("/dashboard")
            assertEquals(HttpStatusCode.Unauthorized, resp.status)
        }

    @Test
    fun dashboardClientForbiddenTest(): Unit =
        withB2bTestApp("dashboard-forbidden-flow") {
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val resp =
                client.get("/dashboard") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.Forbidden, resp.status)
        }
}
