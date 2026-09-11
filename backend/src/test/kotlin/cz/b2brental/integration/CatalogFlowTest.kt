@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CatalogFlowTest {
    @Test
    fun catalogAccessTest(): Unit =
        withB2bTestApp("catalog-flow") {
            val unauthResp = client.get("/catalog")
            assertEquals(HttpStatusCode.Unauthorized, unauthResp.status)

            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val catalogResp =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, catalogResp.status)

            val techToken = login("tech@b2b.demo", "tech12345abcd")
            val techCatalog =
                client.get("/catalog") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.OK, techCatalog.status)
        }
}
