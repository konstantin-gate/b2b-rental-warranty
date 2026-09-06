@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Severity
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.services.llm.DisabledLlmClient
import cz.b2brental.services.llm.LlmClient
import cz.b2brental.services.llm.LlmCompletionRequest
import cz.b2brental.services.llm.LlmCompletionResult
import cz.b2brental.services.llm.LlmSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Falešný klient LLM vracející pevně danou odpověď a zaznamenávající přijaté požadavky (bez sítě)
 * @property response pevná odpověď vrácená na každý požadavek
 */
private class FakeLlmClient(
    private val response: String,
) : LlmClient {
    /** Zaznamenané požadavky předané klientovi */
    val requests: MutableList<LlmCompletionRequest> = mutableListOf()

    override suspend fun complete(request: LlmCompletionRequest): LlmCompletionResult {
        requests.add(request)
        return LlmSuccess(response)
    }

    override fun close(): Unit = Unit
}

class AiServiceTest {
    @Test
    fun fallbackDiagnoseWithoutKeyTest(): Unit =
        runBlocking {
            val service = AiService(DisabledLlmClient)
            val result = service.diagnose("Kompresor hučí", null, KnowledgeContext(emptyList()))
            assertEquals(Severity.medium, result.severity)
            assertTrue(result.possibleCause.contains("AI není dostupná"))
            assertNotNull(result.recommendation)
            service.close()
        }

    @Test
    fun fallbackExplainVerdictWithoutKeyTest(): Unit =
        runBlocking {
            val service = AiService(DisabledLlmClient)
            val result =
                service.explainVerdict(WarrantyVerdict.covered, "Záruka platí", KnowledgeContext(emptyList()))
            assertTrue(result.contains("AI není dostupná"))
            assertTrue(result.contains("Záruka platí"))
            service.close()
        }

    @Test
    fun fallbackAssistantReplyWithoutKeyTest(): Unit =
        runBlocking {
            val service = AiService(DisabledLlmClient)
            val result =
                service.assistantReply("Jaké jsou metriky?", "Aktivní smlouvy: 5", KnowledgeContext(emptyList()))
            assertEquals("AI asistent není dostupný", result)
            service.close()
        }

    @Test
    fun parseValidJsonDiagnosisTest(): Unit =
        runBlocking {
            val service =
                AiService(
                    FakeLlmClient("{\"possible_cause\":\"X\",\"severity\":\"low\",\"recommendation\":\"Y\"}"),
                )
            val result = service.diagnose("Kompresor hučí", null, KnowledgeContext(emptyList()))
            assertEquals("X", result.possibleCause)
            assertEquals(Severity.low, result.severity)
            assertEquals("Y", result.recommendation)
            service.close()
        }

    @Test
    fun parseMarkdownFenceAndThinkTest(): Unit =
        runBlocking {
            val raw: String =
                "<think>vnitřní úvaha</think>```json\n" +
                    "{\"possible_cause\":\"Závada kompresoru\",\"severity\":\"critical\",\"recommendation\":\"Zavolejte technika\"}\n```"
            val service = AiService(FakeLlmClient(raw))
            val result = service.diagnose("Kompresor hučí", null, KnowledgeContext(emptyList()))
            assertEquals("Závada kompresoru", result.possibleCause)
            assertEquals(Severity.critical, result.severity)
            service.close()
        }

    @Test
    fun unknownSeverityFallsBackToMediumTest(): Unit =
        runBlocking {
            val service =
                AiService(
                    FakeLlmClient("{\"possible_cause\":\"X\",\"severity\":\"nope\",\"recommendation\":\"Y\"}"),
                )
            val result = service.diagnose("Kompresor hučí", null, KnowledgeContext(emptyList()))
            assertEquals(Severity.medium, result.severity)
            service.close()
        }

    @Test
    fun malformedJsonFallsBackTest(): Unit =
        runBlocking {
            val service = AiService(FakeLlmClient("tohle není JSON"))
            val result = service.diagnose("Kompresor hučí", null, KnowledgeContext(emptyList()))
            assertEquals(Severity.medium, result.severity)
            assertTrue(result.possibleCause.contains("AI není dostupná"))
            service.close()
        }

    @Test
    fun diagnosePromptContainsKnowledgeBlockTest(): Unit =
        runBlocking {
            val client =
                FakeLlmClient("{\"possible_cause\":\"X\",\"severity\":\"low\",\"recommendation\":\"Y\"}")
            val service = AiService(client)
            val context =
                KnowledgeContext(
                    listOf(KnowledgeSnippet("[KB:warranty-rules#1]", "Nadpis", "Obsah", 10)),
                )
            service.diagnose("Kompresor hučí", null, context)
            val systemMessage =
                client.requests[0].messages.first { message -> message.role == "system" }
            assertTrue(systemMessage.content.contains("[KB:warranty-rules#1]"))
            service.close()
        }

    @Test
    fun diagnoseCitationFilterTest(): Unit =
        runBlocking {
            val raw: String =
                "{\"possible_cause\":\"X\",\"severity\":\"low\"," +
                    "\"recommendation\":\"Vyměňte kompresor [KB:warranty-rules#1] dle [KB:fake#9]\"}"
            val service = AiService(FakeLlmClient(raw))
            val context =
                KnowledgeContext(
                    listOf(KnowledgeSnippet("[KB:warranty-rules#1]", "Nadpis", "Obsah", 10)),
                )
            val result = service.diagnose("Kompresor hučí", null, context)
            assertTrue(result.recommendation.contains("[KB:warranty-rules#1]"))
            assertTrue(!result.recommendation.contains("[KB:fake#9]"))
            service.close()
        }

    @Test
    fun closeClientTest() {
        val service = AiService(DisabledLlmClient)
        service.close()
        // Opakované zavolání close nesmí vyhodit výjimku
        service.close()
    }
}
