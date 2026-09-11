@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthFlowTest {
    @Test
    fun fullAuthFlow(): Unit =
        withB2bTestApp("auth-flow") {
            val regResp =
                client.post("/auth/register-company") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"companyName\":\"Test Corp\",\"inn\":\"28745019\"," +
                            "\"address\":\"Praha\",\"adminEmail\":\"newadm@test.cz\"," +
                            "\"password\":\"secret1234abcd\"}",
                    )
                }
            assertEquals(HttpStatusCode.Created, regResp.status)

            val token = login("admin@b2b.demo", "admin1234abcd")
            assertTrue(token.isNotBlank())

            val failResp =
                client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"email\":\"admin@b2b.demo\",\"password\":\"wrongpass\"}")
                }
            assertEquals(HttpStatusCode.Unauthorized, failResp.status)

            val unauthCatalog = client.get("/catalog")
            assertEquals(HttpStatusCode.Unauthorized, unauthCatalog.status)

            val techToken = login("tech@b2b.demo", "tech12345abcd")
            val forbiddenAdd =
                client.post("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"categoryId\":1,\"model\":\"M\",\"serialNumber\":\"SN99\"," +
                            "\"price\":\"1000.00\",\"monthlyRate\":\"100.00\"}",
                    )
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenAdd.status)
        }
}
