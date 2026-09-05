@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services.llm

import cz.b2brental.config.Config
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Výsledky mockovaného serveru: fronta odpovědí a seznam požadavků
 * @property responses fronta odpovědí (status, tělo) v pořadí vydání
 * @property requests zaznamenané HTTP požadavky
 */
private class MockResults(
    val responses: MutableList<Pair<HttpStatusCode, String>>,
) {
    val requests: MutableList<HttpRequestData> = mutableListOf()
}

/** Vytvoří klienta LLM na MockEngine s frontou odpovědí */
private fun makeClient(
    config: Config,
    results: MockResults,
): OpenAiCompatibleLlmClient {
    val handler: MockRequestHandler = { request ->
        results.requests.add(request)
        val (status, body) =
            if (results.responses.size > 1) results.responses.removeAt(0) else results.responses[0]
        respond(
            content = ByteReadChannel(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    return OpenAiCompatibleLlmClient(config, engine = MockEngine(handler))
}

/** Testy OpenAiCompatibleLlmClient na MockEngine */
class OpenAiCompatibleLlmClientTest {
    private fun config(
        aiApiKey: String?,
        aiMaxRetries: Int = 0,
    ): Config =
        Config(
            dbUrl = "jdbc:h2:mem:x;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            dbUser = "sa",
            dbPass = "",
            jwtSecret = "test-secret-32-znaku-minimum-pro-hs256",
            aiBaseUrl = "http://127.0.0.1:8080/v1/",
            aiModel = "test-model",
            aiApiKey = aiApiKey,
            aiRequestTimeoutMillis = 30000L,
            aiConnectTimeoutMillis = 5000L,
            aiMaxRetries = aiMaxRetries,
            aiMaxOutputTokens = 700,
            aiEnabled = true,
        )

    private fun jsonQuote(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun okBody(content: String): String = "{\"choices\":[{\"message\":{\"content\":" + jsonQuote(content) + "}}]}"

    @Test
    fun completionUrlAndNoAuthorizationTest(): Unit =
        runBlocking {
            val results = MockResults(mutableListOf(HttpStatusCode.OK to okBody("odpověď")))
            val client = makeClient(config(aiApiKey = null), results)
            val result = client.complete(LlmCompletionRequest(listOf(LlmMessage("user", "ahoj"))))
            assertIs<LlmSuccess>(result)
            assertEquals("odpověď", result.text)
            val request: HttpRequestData = results.requests.single()
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.toString().endsWith("/chat/completions"))
            assertNull(request.headers[HttpHeaders.Authorization])
            client.close()
        }

    @Test
    fun authorizationHeaderPresentWhenKeySetTest(): Unit =
        runBlocking {
            val results = MockResults(mutableListOf(HttpStatusCode.OK to okBody("odpověď")))
            val client = makeClient(config(aiApiKey = "secret-key"), results)
            client.complete(LlmCompletionRequest(listOf(LlmMessage("user", "ahoj"))))
            val request: HttpRequestData = results.requests.single()
            assertEquals("Bearer secret-key", request.headers[HttpHeaders.Authorization])
            client.close()
        }

    @Test
    fun thinkBlocksRemovedTest(): Unit =
        runBlocking {
            val body: String = "<think>úvaha</think>čistý text"
            val results = MockResults(mutableListOf(HttpStatusCode.OK to okBody(body)))
            val client = makeClient(config(aiApiKey = null), results)
            val result = client.complete(LlmCompletionRequest(listOf(LlmMessage("user", "ahoj"))))
            assertIs<LlmSuccess>(result)
            assertEquals("čistý text", result.text)
            client.close()
        }

    @Test
    fun http500RetriedTest(): Unit =
        runBlocking {
            val results =
                MockResults(
                    mutableListOf(
                        HttpStatusCode.InternalServerError to "{}",
                        HttpStatusCode.OK to okBody("po opakování"),
                    ),
                )
            val client = makeClient(config(aiApiKey = null, aiMaxRetries = 1), results)
            val result = client.complete(LlmCompletionRequest(listOf(LlmMessage("user", "ahoj"))))
            assertIs<LlmSuccess>(result)
            assertEquals("po opakování", result.text)
            assertEquals(2, results.requests.size)
            client.close()
        }

    @Test
    fun http400NotRetriedTest(): Unit =
        runBlocking {
            val results = MockResults(mutableListOf(HttpStatusCode.BadRequest to "{}"))
            val client = makeClient(config(aiApiKey = null, aiMaxRetries = 1), results)
            val result = client.complete(LlmCompletionRequest(listOf(LlmMessage("user", "ahoj"))))
            assertIs<LlmFailure>(result)
            assertEquals(1, results.requests.size)
            client.close()
        }

    @Test
    fun emptyChoicesFailureTest(): Unit =
        runBlocking {
            val results = MockResults(mutableListOf(HttpStatusCode.OK to "{\"choices\":[]}"))
            val client = makeClient(config(aiApiKey = null), results)
            val result = client.complete(LlmCompletionRequest(listOf(LlmMessage("user", "ahoj"))))
            assertIs<LlmFailure>(result)
            client.close()
        }
}
