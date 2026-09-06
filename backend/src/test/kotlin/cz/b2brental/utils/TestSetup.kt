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
import org.jetbrains.exposed.sql.transactions.TransactionManager

public fun withB2bTestApp(
    dbName: String,
    block: suspend ApplicationTestBuilder.() -> Unit,
) {
    runBlocking {
        // Reset pinu před celým testem: tělo testu (block) i modul startují až později,
        // a bez resetu by dědily ThreadLocal pin z předchozího testu.
        TransactionManager.resetCurrent(null)
        testApplication {
            application {
                // Starý pin Exposed (ThreadLocal TransactionManager z jiných testů)
                // by způsobil, že module() zapíše schéma do cizí databáze; pin se proto resetuje,
                // aby transaction{} znovu přebral posledně registrovanou databázi modulu.
                TransactionManager.resetCurrent(null)
                module(
                    Config(
                        dbUrl = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        dbUser = "sa",
                        dbPass = "",
                        jwtSecret = "test-secret-32-znaku-minimum-pro-hs256",
                        aiBaseUrl = "http://127.0.0.1:8080/v1",
                        aiModel = "test-model",
                        aiApiKey = null,
                        aiRequestTimeoutMillis = 30000L,
                        aiConnectTimeoutMillis = 5000L,
                        aiMaxRetries = 0,
                        aiMaxOutputTokens = 700,
                        aiEnabled = false,
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
