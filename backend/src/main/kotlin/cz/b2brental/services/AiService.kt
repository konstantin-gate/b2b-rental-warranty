@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Severity
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.services.llm.LlmClient
import cz.b2brental.services.llm.LlmCompletionRequest
import cz.b2brental.services.llm.LlmFailure
import cz.b2brental.services.llm.LlmMessage
import cz.b2brental.services.llm.LlmSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Výsledek diagnostiky AI
 * @property possibleCause pravděpodobní příčina závady
 * @property severity závažnost závady (low, medium, critical)
 * @property recommendation doporučené následující kroky
 */
public data class DiagnosisResult(
    public val possibleCause: String,
    public val severity: Severity,
    public val recommendation: String,
)

/**
 * Služba AI: příprava promptů, volání LLM klienta a deterministický fallback.
 * HTTP komunikaci deleguje na [LlmClient], sám síťová volání nevykonává.
 * @property llmClient klient LLM zajišťující síťová volání
 */
public class AiService(
    private val llmClient: LlmClient,
) : AutoCloseable {
    /** Logger pro události služby */
    private val log = LoggerFactory.getLogger(AiService::class.java)

    public companion object {
        /** Maximální délka zalogovaného textu chyby */
        private const val LOG_LIMIT: Int = 250

        /** Povolené hodnoty závažnosti přijímané z odpovědi LLM */
        private val ALLOWED_SEVERITY: Set<String> = setOf("low", "medium", "critical")
    }

    /** Uvolní prostředky podřízeného LLM klienta */
    override fun close(): Unit = llmClient.close()

    /**
     * Provede dokončení konverzace přes klienta LLM.
     * @param messages zprávy konverzace k odeslání modelu
     * @return text odpovědi nebo null při selhání (včetně vypnutého AI)
     */
    private suspend fun chat(messages: List<LlmMessage>): String? =
        try {
            val result =
                llmClient.complete(
                    LlmCompletionRequest(messages = messages),
                )
            when (result) {
                is LlmSuccess -> result.text
                is LlmFailure -> {
                    log.warn("Volání LLM selhalo: ${result.message.take(LOG_LIMIT)}")
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Volání LLM selhalo: ${e.message.orEmpty().take(LOG_LIMIT)}")
            null
        }

    /**
     * Diagnostika poruchy: LLM s deterministickým fallbackem.
     * Fotografie se na LLM neposílá jako obrázek - pouze textové upozornění.
     * @param description textový popis závady od klienta
     * @param photoBase64 base64 fotografie závady nebo null
     * @return výsledek diagnostiky
     */
    public suspend fun diagnose(
        description: String,
        photoBase64: String?,
    ): DiagnosisResult {
        val systemPrompt: String =
            "Jsi inženýr pro diagnostiku komerčního chladicího zařízení. " +
                "Odpovídej VÝHRADNĚ platným JSON objektem ve tvaru: " +
                "{\"possible_cause\":\"...\",\"severity\":\"low|medium|critical\",\"recommendation\":\"...\"}. " +
                "Žádný jiný text."

        val userContent: String =
            if (photoBase64.isNullOrBlank()) {
                "Popis závady: $description"
            } else {
                "Popis závady: $description (fotografie přiložena, model obraz neuvažuje)"
            }

        val answer: String? =
            chat(
                listOf(
                    LlmMessage("system", systemPrompt),
                    LlmMessage("user", userContent),
                ),
            )
        if (answer == null) return fallbackDiagnose()

        return parseDiagnosis(answer)
    }

    /**
     * Vysvětlení deterministického verdiktu LLM v češtině (2 věty).
     * @param verdict verdikt garance z pravidlového enginu
     * @param reason odůvodnění verdiktu pravidly nebo null
     * @return text vysvětlení (s fallbackem při nedostupnosti AI)
     */
    public suspend fun explainVerdict(
        verdict: WarrantyVerdict,
        reason: String?,
    ): String {
        val answer: String? =
            chat(
                listOf(
                    LlmMessage(
                        "system",
                        "Jsi zákaznický poradce půjčovny chladicího zařízení. Vysvětli max. ve 2 větách česky, " +
                            "proč záruční verdikt dopadl takto. Pouze vysvětlení, žádné další texty.",
                    ),
                    LlmMessage("user", "Verdikt: ${verdict.name}. Odůvodnění pravidly: ${reason ?: "neuvedeno"}"),
                ),
            )
        return answer?.trim().takeUnless { it.isNullOrEmpty() }
            ?: "AI není dostupná — verdikt vypočítán pravidly: ${reason ?: "bez dodatečného odůvodnění"}"
    }

    /**
     * Odpověď AI asistenta s kontextem metrik.
     * @param message dotaz uživatele
     * @param context kontext dat systému pro odpověď
     * @return text odpovědi asistenta (s fallbackem při nedostupnosti AI)
     */
    public suspend fun assistantReply(
        message: String,
        context: String,
    ): String {
        val answer: String? =
            chat(
                listOf(
                    LlmMessage(
                        "system",
                        "Jsi AI asistent manažera půjčovny komerčního chladicího zařízení. " +
                            "Odpovídej česky na základě uvedených dat systému.\n" +
                            "Kontext dat systému:\n$context",
                    ),
                    LlmMessage("user", message),
                ),
            )
        return answer?.trim().takeUnless { it.isNullOrEmpty() } ?: "AI asistent není dostupný"
    }

    /** Deterministický fallback diagnostiky při nedostupnosti AI */
    private fun fallbackDiagnose(): DiagnosisResult =
        DiagnosisResult(
            possibleCause = "Příčina nebyla určena — AI není dostupná",
            severity = Severity.medium,
            recommendation = "Vyžaduje ruční diagnostiku technikem",
        )

    /**
     * Rozparsuje odpověď LLM na výsledek diagnostiky s fallbackem při chybě.
     * @param answer text odpovědi LLM
     * @return výsledek diagnostiky
     */
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
        } catch (_: Exception) {
            fallbackDiagnose()
        }
    }

    /**
     * Vyhledá a rozparsuje první JSON objekt v textu odpovědi.
     * @param answer text odpovědi LLM
     * @return rozparsovaný JSON objekt nebo null, pokud v textu není
     */
    private fun extractJsonObject(answer: String): JsonObject? {
        return try {
            val start = answer.indexOf('{')
            val end = answer.lastIndexOf('}')
            if (start == -1 || end == -1 || start >= end) return null
            val cleaned = answer.substring(start, end + 1)
            Json.parseToJsonElement(cleaned).jsonObject
        } catch (_: Exception) {
            null
        }
    }
}
