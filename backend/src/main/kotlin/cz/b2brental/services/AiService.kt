@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Severity
import cz.b2brental.db.WarrantyVerdict
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/** Výsledek diagnostiky AI */
public data class DiagnosisResult(
    public val possibleCause: String,
    public val severity: Severity,
    public val recommendation: String,
)

/** Služba AI: volání OpenAI chat completions s deterministickým fallbackem */
public class AiService(
    private val openaiApiKey: String?,
) : AutoCloseable {
    private val log = LoggerFactory.getLogger(AiService::class.java)

    private val client: HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000L
                connectTimeoutMillis = 10_000L
                socketTimeoutMillis = 30_000L
            }
        }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.2,
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: JsonElement,
    )

    @Serializable
    private data class ChoiceMessage(
        val content: String? = null,
    )

    @Serializable
    private data class Choice(
        val message: ChoiceMessage,
    )

    @Serializable
    private data class ChatResponse(
        val choices: List<Choice> = emptyList(),
    )

    public companion object {
        private const val MODEL: String = "gpt-4o-mini"
        private const val CHAT_URL: String = "https://api.openai.com/v1/chat/completions"
        private const val LOG_LIMIT: Int = 250
        private val ALLOWED_SEVERITY: Set<String> = setOf("low", "medium", "critical")
    }

    override fun close() {
        client.close()
    }

    private suspend fun chat(messages: List<ChatMessage>): String? {
        val key: String = openaiApiKey ?: return null
        return try {
            val response: ChatResponse =
                client
                    .post(CHAT_URL) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $key")
                        setBody(ChatRequest(model = MODEL, messages = messages))
                    }.body()
            response.choices
                .firstOrNull()
                ?.message
                ?.content
        } catch (e: Throwable) {
            log.warn("OpenAI volání selhalo: ${e.message.orEmpty().take(LOG_LIMIT)}")
            null
        }
    }

    /** Diagnostika poruchy: LLM s fallbackem a podporou multimodálních zpráv */
    public suspend fun diagnose(
        description: String,
        photoBase64: String?,
    ): DiagnosisResult {
        val systemPrompt =
            "Jsi inženýr pro diagnostiku komerčního chladicího zařízení. " +
                "Odpovídej VÝHRADNĚ platným JSON objektem ve tvaru: " +
                "{\"possible_cause\":\"...\",\"severity\":\"low|medium|critical\",\"recommendation\":\"...\"}. " +
                "Žádný jiný text."

        val userContent: JsonElement =
            if (photoBase64.isNullOrBlank()) {
                JsonPrimitive("Popis závady: $description")
            } else {
                val dataUrl = if (photoBase64.startsWith("data:")) photoBase64 else "data:image/jpeg;base64,$photoBase64"
                buildJsonArray {
                    add(
                        JsonObject(
                            mapOf(
                                "type" to JsonPrimitive("text"),
                                "text" to JsonPrimitive("Popis závady: $description"),
                            ),
                        ),
                    )
                    add(
                        JsonObject(
                            mapOf(
                                "type" to JsonPrimitive("image_url"),
                                "image_url" to JsonObject(mapOf("url" to JsonPrimitive(dataUrl))),
                            ),
                        ),
                    )
                }
            }

        val answer: String =
            chat(
                listOf(
                    ChatMessage("system", JsonPrimitive(systemPrompt)),
                    ChatMessage("user", userContent),
                ),
            ) ?: return fallbackDiagnose()

        return parseDiagnosis(answer)
    }

    /** Vysvětlení deterministického verdiktu LLM v češtině (2 věty) */
    public suspend fun explainVerdict(
        verdict: WarrantyVerdict,
        reason: String?,
    ): String {
        val answer: String? =
            chat(
                listOf(
                    ChatMessage(
                        "system",
                        JsonPrimitive(
                            "Jsi zákaznický poradce půjčovny chladicího zařízení. Vysvětli max. ve 2 větách česky, " +
                                "proč záruční verdikt dopadl takto. Pouze vysvětlení, žádné další texty.",
                        ),
                    ),
                    ChatMessage("user", JsonPrimitive("Verdikt: ${verdict.name}. Odůvodnění pravidly: ${reason ?: "neuvedeno"}")),
                ),
            )
        return answer?.trim().takeUnless { it.isNullOrEmpty() }
            ?: "AI není dostupná — verdikt vypočítán pravidly: ${reason ?: "bez dodatečného odůvodnění"}"
    }

    /** Odpověď AI asistenta s kontextem metrik */
    public suspend fun assistantReply(
        message: String,
        context: String,
    ): String {
        val answer: String? =
            chat(
                listOf(
                    ChatMessage(
                        "system",
                        JsonPrimitive(
                            "Jsi AI asistent manažera půjčovny komerčního chladicího zařízení. " +
                                "Odpovídej česky na základě uvedených dat systému.\n" +
                                "Kontext dat systému:\n$context",
                        ),
                    ),
                    ChatMessage("user", JsonPrimitive(message)),
                ),
            )
        return answer?.trim().takeUnless { it.isNullOrEmpty() } ?: "AI asistent není dostupný"
    }

    private fun fallbackDiagnose(): DiagnosisResult =
        DiagnosisResult(
            possibleCause = "Příčina nebyla určena — AI není dostupná",
            severity = Severity.medium,
            recommendation = "Vyžaduje ruční diagnostiku technikem",
        )

    private fun parseDiagnosis(answer: String): DiagnosisResult {
        val json: JsonObject = extractJsonObject(answer) ?: return fallbackDiagnose()
        return try {
            val cause: String =
                json["possible_cause"]?.jsonPrimitive?.contentOrNull ?: return fallbackDiagnose()
            val severityRaw: String =
                json["severity"]?.jsonPrimitive?.contentOrNull ?: "medium"
            val severity: Severity =
                if (severityRaw in ALLOWED_SEVERITY) Severity.valueOf(severityRaw) else Severity.medium
            val recommendation: String =
                json["recommendation"]?.jsonPrimitive?.contentOrNull
                    ?: "Vyžaduje ruční diagnostiku technikem"
            DiagnosisResult(cause, severity, recommendation)
        } catch (_: Throwable) {
            fallbackDiagnose()
        }
    }

    private fun extractJsonObject(answer: String): JsonObject? {
        return try {
            val start = answer.indexOf('{')
            val end = answer.lastIndexOf('}')
            if (start == -1 || end == -1 || start >= end) return null
            val cleaned = answer.substring(start, end + 1)
            Json.parseToJsonElement(cleaned).jsonObject
        } catch (_: Throwable) {
            null
        }
    }
}
