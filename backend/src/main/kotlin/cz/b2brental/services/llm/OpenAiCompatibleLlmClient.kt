@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services.llm

import cz.b2brental.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Klient LLM kompatibilní s OpenAI API (llama-server).
 * Provádí HTTP volání /chat/completions s časovými limity, omezením délky
 * vstupu a omezenými opakováními (timeout, chyba spojení, 429, 5xx).
 * @property config konfigurace připojení k LLM (URL, model, timeouty)
 * @param engine HTTP engine klienta nebo null pro výchozí CIO
 */
public class OpenAiCompatibleLlmClient internal constructor(
    private val config: Config,
    engine: HttpClientEngine?,
) : LlmClient {
    /**
     * Vytvoří klienta s výchozím CIO enginem podle konfigurace.
     * @param config konfigurace připojení k LLM
     */
    public constructor(config: Config) : this(config, null)

    /** Logger pro události klienta */
    private val log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient::class.java)

    /** Normalizovaná URL pro completion (bez koncového lomítka) */
    private val completionUrl: String = config.aiBaseUrl.trimEnd('/') + "/chat/completions"

    /** HTTP klient s nastavenými timeouty a JSON serializací */
    private val client: HttpClient =
        HttpClient(engine ?: CIO.create()) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.aiRequestTimeoutMillis
                connectTimeoutMillis = config.aiConnectTimeoutMillis
                socketTimeoutMillis = config.aiRequestTimeoutMillis
            }
        }

    /**
     * Tělo požadavku /chat/completions (OpenAI formát).
     * @property model název modelu LLM
     * @property messages zprávy konverzace
     * @property temperature teplota generování
     * @property maxTokens limit výstupních tokenů (v JSON jako max_tokens)
     * @property stream příznak streamování (vždy false)
     */
    @Serializable
    private data class CompletionBody(
        val model: String,
        val messages: List<MessageBody>,
        val temperature: Double,
        @SerialName("max_tokens") val maxTokens: Int,
        val stream: Boolean,
    )

    /**
     * Zpráva konverzace v těle požadavku.
     * @property role role odesílatele
     * @property content text zprávy
     */
    @Serializable
    private data class MessageBody(
        val role: String,
        val content: String,
    )

    /**
     * Text odpovědi modelu v odpovědi serveru.
     * @property content text odpovědi nebo null
     */
    @Serializable
    private data class ChoiceMessage(
        val content: String? = null,
    )

    /**
     * Jedna volba odpovědi serveru.
     * @property message zpráva modelu
     */
    @Serializable
    private data class Choice(
        val message: ChoiceMessage,
    )

    /**
     * Tělo odpovědi /chat/completions (OpenAI formát).
     * @property choices seznam voleb odpovědi
     */
    @Serializable
    private data class ChatResponse(
        val choices: List<Choice> = emptyList(),
    )

    /** Uvolní HTTP klienta */
    override fun close(): Unit = client.close()

    /**
     * Provede dokončení konverzace modelem přes llama-server.
     * Opakuje pouze při timeoutu, chybě spojení, HTTP 429 a HTTP 5xx.
     * @param request požadavek se zprávami konverzace
     * @return text odpovědi bez bloků think nebo popis selhání
     */
    override suspend fun complete(request: LlmCompletionRequest): LlmCompletionResult {
        val messages: List<MessageBody> = prepareMessages(request.messages)
        val body: CompletionBody =
            CompletionBody(
                model = config.aiModel,
                messages = messages,
                temperature = 0.1,
                maxTokens = config.aiMaxOutputTokens,
                stream = false,
            )

        var attempt: Int = 0
        var lastFailure: String = "AI není dostupná"
        while (attempt <= config.aiMaxRetries) {
            try {
                val response: HttpResponse =
                    client.post(completionUrl) {
                        contentType(ContentType.Application.Json)
                        if (!config.aiApiKey.isNullOrBlank()) {
                            header(HttpHeaders.Authorization, "Bearer ${config.aiApiKey}")
                        }
                        setBody(body)
                    }
                val status: HttpStatusCode = response.status
                if (status.value == 429 || status.value in 500..599) {
                    lastFailure = "LLM server vrátil HTTP ${status.value}"
                    log.warn("Volání LLM selhalo (pokus ${attempt + 1}): $lastFailure")
                    attempt++
                    continue
                }
                if (!status.isSuccess()) {
                    return LlmFailure("LLM server vrátil HTTP ${status.value}")
                }
                return parseResponse(response.bodyAsText())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message: String = (e.message ?: e::class.simpleName.orEmpty()).take(LOG_LIMIT)
                lastFailure = message.ifEmpty { "AI není dostupná" }
                val retryable: Boolean =
                    e is HttpRequestTimeoutException ||
                        e is ConnectTimeoutException ||
                        e is java.net.SocketTimeoutException ||
                        e is java.net.SocketException
                log.warn("Volání LLM selhalo (pokus ${attempt + 1}): $message")
                if (!retryable) {
                    return LlmFailure(lastFailure)
                }
            }
            attempt++
        }
        return LlmFailure(lastFailure)
    }

    /**
     * Sestaví zprávy požadavku; pokud součet délek content přesáhne limit,
     * zkrátí poslední user zprávu. Obsah promptů se neloguje.
     * @param messages zprávy konverzace z požadavku
     * @return zprávy v těle HTTP požadavku
     */
    @Suppress("KDocMissingDocumentation")
    private fun prepareMessages(messages: List<LlmMessage>): List<MessageBody> {
        val prepared: MutableList<MessageBody> =
            messages.map { message -> MessageBody(role = message.role, content = message.content) }.toMutableList()
        val totalLength: Int = prepared.sumOf { message -> message.content.length }
        if (totalLength > MAX_INPUT_LENGTH) {
            val lastIndex: Int = prepared.indexOfLast { message -> message.role == "user" }
            if (lastIndex >= 0) {
                val overflow: Int = totalLength - MAX_INPUT_LENGTH
                val content: String = prepared[lastIndex].content
                val truncated: String = content.take((content.length - overflow).coerceAtLeast(0))
                prepared[lastIndex] = MessageBody(role = "user", content = truncated)
            }
        }
        return prepared
    }

    /**
     * Rozparsuje tělo odpovědi a vrátí výsledek.
     * Chybějící/pusty `choices` nebo prázdný text znamená selhání.
     * @param raw tělo HTTP odpovědi jako text
     * @return [LlmSuccess] s vyčištěným textem nebo [LlmFailure] s popisem chyby
     */
    private fun parseResponse(raw: String): LlmCompletionResult {
        val json: JsonObject =
            try {
                Json.parseToJsonElement(raw).jsonObject
            } catch (_: Exception) {
                return LlmFailure("Neplatná odpověď LLM serveru")
            }
        val content: String? =
            runCatching {
                val choices = json["choices"]?.jsonArray ?: return@runCatching null
                val first = choices.firstOrNull()?.jsonObject ?: return@runCatching null
                val message = first["message"]?.jsonObject ?: return@runCatching null
                message["content"]?.jsonPrimitive?.contentOrNull
            }.getOrNull()
        if (content.isNullOrEmpty()) {
            return LlmFailure("LLM vrátila prázdnou odpověď")
        }
        val cleaned: String = content.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
        if (cleaned.isEmpty()) {
            return LlmFailure("LLM vrátila prázdnou odpověď")
        }
        return LlmSuccess(cleaned)
    }

    public companion object {
        /** Limit souhrnné délky vstupních zpráv ve znacích */
        private const val MAX_INPUT_LENGTH: Int = 24_000

        /** Limit délky textu chyby z externího API */
        private const val LOG_LIMIT: Int = 250
    }
}
