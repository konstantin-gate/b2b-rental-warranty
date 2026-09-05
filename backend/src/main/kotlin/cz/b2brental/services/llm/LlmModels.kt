@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services.llm

/**
 * Zpráva konverzace pro LLM
 * @property role role odesílatele (system, user)
 * @property content text zprávy
 */
public data class LlmMessage(
    public val role: String,
    public val content: String,
)

/**
 * Požadavek na dokončení konverzace LLM
 * @property messages zprávy konverzace v pořadí odeslání
 */
public data class LlmCompletionRequest(
    public val messages: List<LlmMessage>,
)

/** Výsledek dokončení konverzace LLM */
public sealed interface LlmCompletionResult

/**
 * Úspěšné dokončení s textem odpovědi modelu
 * @property text text odpovědi modelu
 */
public data class LlmSuccess(
    public val text: String,
) : LlmCompletionResult

/**
 * Neúspěšné dokončení s popisem chyby
 * @property message popis chyby (vhodný pro logování)
 */
public data class LlmFailure(
    public val message: String,
) : LlmCompletionResult
