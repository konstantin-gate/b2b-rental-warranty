@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.utils

import cz.b2brental.config.Config
import cz.b2brental.module
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking

public fun withB2bTestApp(
    dbName: String,
    block: suspend ApplicationTestBuilder.() -> Unit,
) {
    runBlocking {
        testApplication {
            application {
                module(
                    Config(
                        dbUrl = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        dbUser = "sa",
                        dbPass = "",
                        jwtSecret = "test-secret-32-znaku-minimum-pro-hs256",
                        openaiApiKey = null,
                    ),
                )
            }
            block()
        }
    }
}

public suspend fun ApplicationTestBuilder.login(
    email: String,
    pass: String,
): String {
    val resp =
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{\"email\":\"$email\",\"password\":\"$pass\"}")
        }
    val text = resp.bodyAsText()
    val match = Regex("\"token\":\"([^\"]+)\"").find(text)
    return match?.groupValues?.get(1) ?: error("Nepodařilo se získat token z odpovědi: $text")
}
