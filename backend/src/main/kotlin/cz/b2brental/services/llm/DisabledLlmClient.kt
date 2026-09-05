@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services.llm

/** Vypnutý klient LLM bez síťových volání - vždy vrací selhání */
public object DisabledLlmClient : LlmClient {
    /**
     * Vždy vrací selhání - AI je vypnutá.
     * @param request požadavek na dokončení (ignorován)
     * @return vždy [LlmFailure] s hlášením nedostupnosti AI
     */
    override suspend fun complete(request: LlmCompletionRequest): LlmCompletionResult = LlmFailure("AI není dostupná")

    /** Uvolnění prostředků - u vypnutého klienta není co uvolňovat */
    override fun close(): Unit = Unit
}
