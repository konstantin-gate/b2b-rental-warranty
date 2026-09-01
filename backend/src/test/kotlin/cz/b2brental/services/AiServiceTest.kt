@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Severity
import cz.b2brental.db.WarrantyVerdict
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiServiceTest {
    @Test
    fun fallbackDiagnoseWithoutKeyTest(): Unit =
        runBlocking {
            val service = AiService(null)
            val result = service.diagnose("Kompresor hučí", null)
            assertEquals(Severity.medium, result.severity)
            assertTrue(result.possibleCause.contains("AI není dostupná"))
            assertNotNull(result.recommendation)
            service.close()
        }

    @Test
    fun fallbackExplainVerdictWithoutKeyTest(): Unit =
        runBlocking {
            val service = AiService(null)
            val result = service.explainVerdict(WarrantyVerdict.covered, "Záruka platí")
            assertTrue(result.contains("AI není dostupná"))
            assertTrue(result.contains("Záruka platí"))
            service.close()
        }

    @Test
    fun fallbackAssistantReplyWithoutKeyTest(): Unit =
        runBlocking {
            val service = AiService(null)
            val result = service.assistantReply("Jaké jsou metriky?", "Aktivní smlouvy: 5")
            assertEquals("AI asistent není dostupný", result)
            service.close()
        }

    @Test
    fun closeClientTest() {
        val service = AiService(null)
        service.close()
        // Double close should not throw
        service.close()
    }
}
