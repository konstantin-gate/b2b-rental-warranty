package cz.b2brental.services.llm

/** Rozhraní klienta LLM: jediná operace dokončení konverzace */
public interface LlmClient : AutoCloseable {
    /**
     * Provede dokončení konverzace modelem.
     * @param request požadavek se zprávami konverzace
     * @return výsledek úspěchu nebo selhání
     */
    public suspend fun complete(request: LlmCompletionRequest): LlmCompletionResult

    /** Uvolní prostředky klienta */
    public override fun close(): Unit
}
