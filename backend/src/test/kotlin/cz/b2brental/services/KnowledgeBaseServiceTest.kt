@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.KnowledgeChunks
import cz.b2brental.db.KnowledgeDocuments
import cz.b2brental.utils.withB2bTestApp
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Název in-memory H2 databáze sdílený s withB2bTestApp */
private const val TEST_DB_NAME: String = "kb-retrieval-test"

/** Testy retrievalu znalostní báze: skóre, řazení, limity a formát prompt bloku */
public class KnowledgeBaseServiceTest {
    /**
     * Připraví samostatné H2 úložiště pro test: vytvoří knowledge tabulky, pinuje
     * transakční manažer na tuto databázi a synchronizuje znalostní dokumenty.
     * @return instance databáze, na kterou je transakční manažer pinován
     */
    private fun prepareKnowledgeStore(): Database {
        val db: Database =
            Database.connect(
                "jdbc:h2:mem:$TEST_DB_NAME;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = "",
            )
        TransactionManager.resetCurrent(db.transactionManager)
        transaction {
            SchemaUtils.create(KnowledgeDocuments, KnowledgeChunks)
        }
        KnowledgeIndexService().synchronize()
        return db
    }

    /** Dotaz s kategorií najde úryvek z warranty-rules na prvním místě */
    @Test
    public fun retrieveByCategoryReturnsWarrantyRulesFirst(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val context: KnowledgeContext =
                KnowledgeBaseService().retrieve(
                    KnowledgeQuery("záruka na komoru", "Chladíci komora", null, null),
                )
            assertTrue(context.snippets.isNotEmpty())
            val firstCitation: String = context.snippets.first().citation
            assertTrue(firstCitation.startsWith("[KB:warranty-rules#"))
        }

    /** Nesmyslný dotaz nevrátí žádný úryvek ani žádný podstrčený dokument */
    @Test
    public fun retrieveWithNonsenseReturnsEmpty(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val context: KnowledgeContext =
                KnowledgeBaseService().retrieve(KnowledgeQuery("xyzabc qwertzu", null, null, null))
            assertTrue(context.snippets.isEmpty())
        }

    /** Dva stejné dotazy vrátí identické seznamy (stabilní pořadí) */
    @Test
    public fun retrieveIsStableAcrossCalls(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val service = KnowledgeBaseService()
            val first: KnowledgeContext =
                service.retrieve(KnowledgeQuery("záruka na komoru", "Chladíci komora", null, null))
            val second: KnowledgeContext =
                service.retrieve(KnowledgeQuery("záruka na komoru", "Chladíci komora", null, null))
            assertEquals(first.snippets, second.snippets)
        }

    /** Chunk s přesnou shodou error_code dostane alespoň 100 bodů a je na prvním místě */
    @Test
    public fun retrieveByErrorCodeScoresAtLeastHundred(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            transaction {
                val docId =
                    KnowledgeDocuments.insertAndGetId { row ->
                        row[slug] = "test-ec"
                        row[title] = "Testovací dokument"
                        row[sourcePath] = "knowledge/test-ec.md"
                        row[sourceSha256] = "test-sha"
                        row[language] = "cs"
                        row[active] = true
                    }
                KnowledgeChunks.insert { row ->
                    row[documentId] = docId
                    row[ordinal] = 1
                    row[errorCode] = "E-TEST"
                    row[heading] = "Testovací chyba"
                    row[content] = "Popis testovací poruchy s kódem E-TEST"
                    row[searchText] = normalizeSearchText("Testovací chyba Popis testovací poruchy s kódem E-TEST")
                }
            }
            val context: KnowledgeContext =
                KnowledgeBaseService().retrieve(KnowledgeQuery("porucha", null, null, "e-test"))
            assertTrue(context.snippets.isNotEmpty())
            assertEquals("[KB:test-ec#1]", context.snippets.first().citation)
            assertTrue(context.snippets.first().score >= 100)
        }

    /** asPromptBlock() vrací prázdný řetězec pro prázdný kontext a citaci pro neprázdný */
    @Test
    public fun asPromptBlockFormatsContext(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val empty: KnowledgeContext =
                KnowledgeBaseService().retrieve(KnowledgeQuery("xyzabc qwertzu", null, null, null))
            assertEquals("", empty.asPromptBlock())
            val filled: KnowledgeContext =
                KnowledgeBaseService().retrieve(
                    KnowledgeQuery("záruka na komoru", "Chladíci komora", null, null),
                )
            assertTrue(filled.asPromptBlock().contains("[KB:warranty-rules#1]"))
        }

    /** Dotaz s překryvem tokenů (opotřebení, kompresor) najde úryvek přes fulltextové skóre */
    @Test
    public fun retrieveByTokenOverlapReturnsSnippet(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val context: KnowledgeContext =
                KnowledgeBaseService().retrieve(KnowledgeQuery("opotřebení kompresoru", null, null, null))
            assertTrue(context.snippets.isNotEmpty())
        }
}
