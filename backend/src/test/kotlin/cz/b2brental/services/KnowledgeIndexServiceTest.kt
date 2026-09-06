@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.KnowledgeChunks
import cz.b2brental.db.KnowledgeDocuments
import cz.b2brental.utils.withB2bTestApp
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.jetbrains.exposed.sql.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Název in-memory H2 databáze sdílený s withB2bTestApp */
private const val TEST_DB_NAME: String = "kb-index-test"

/** Slug znalostního dokumentu používaný testy indexace */
private const val WARRANTY_RULES_SLUG: String = "warranty-rules"

/** Testy indexace znalostní báze: synchronizace, idempotence a chybové stavy */
public class KnowledgeIndexServiceTest {
    /**
     * Připraví samostatné H2 úložiště pro test: vytvoří knowledge tabulky, pinuje
     * transakční manažer na tuto databázi a synchronizuje znalostní dokumenty.
     * Modul s withB2bTestApp se startuje až po těle testu, bez této přípravy by
     * transakce v těle testu směřovaly do databáze předchozího testu.
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

    /**
     * Vrátí identifikátory dokumentu se slugem [WARRANTY_RULES_SLUG].
     * @return seznam identifikátorů dokumentu
     */
    private fun documentIdsFor(): List<EntityID<Long>> =
        KnowledgeDocuments
            .selectAll()
            .where { KnowledgeDocuments.slug eq WARRANTY_RULES_SLUG }
            .map { row -> row[KnowledgeDocuments.id] }

    /**
     * Vrátí počet chunků dokumentu se slugem [WARRANTY_RULES_SLUG].
     * @return počet chunků dokumentu
     */
    private fun chunkCountFor(): Long =
        transaction {
            val ids: List<EntityID<Long>> = documentIdsFor()
            KnowledgeChunks
                .selectAll()
                .where { KnowledgeChunks.documentId inList ids }
                .count()
        }

    /** Po startu modulu musí být 4 aktivní dokumenty a nenulový počet chunků */
    @Test
    public fun synchronizeSeedsFourActiveDocuments(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            transaction {
                val documents: Int =
                    KnowledgeDocuments
                        .selectAll()
                        .where { KnowledgeDocuments.active eq true }
                        .toList()
                        .size
                assertEquals(4, documents)
                val chunks: Long = KnowledgeChunks.selectAll().count()
                assertTrue(chunks > 0)
            }
        }

    /** Opakovaná synchronizace nezmění počet dokumentů ani chunků (idempotence) */
    @Test
    public fun synchronizeIsIdempotent(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val before: Pair<Long, Long> =
                transaction {
                    Pair(
                        KnowledgeDocuments.selectAll().count(),
                        KnowledgeChunks.selectAll().count(),
                    )
                }
            KnowledgeIndexService().synchronize()
            val after: Pair<Long, Long> =
                transaction {
                    Pair(
                        KnowledgeDocuments.selectAll().count(),
                        KnowledgeChunks.selectAll().count(),
                    )
                }
            assertEquals(before, after)
        }

    /** Změna obsahu dokumentu vyvolá přegenerování jeho chunků při další synchronizaci */
    @Test
    public fun synchronizeRegeneratesChangedDocument(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val originalChunkCount: Long = chunkCountFor()
            transaction {
                KnowledgeDocuments.update({ KnowledgeDocuments.slug eq WARRANTY_RULES_SLUG }) { row ->
                    row[sourceSha256] = "deadbeef"
                }
                KnowledgeChunks.deleteWhere { KnowledgeChunks.documentId inList documentIdsFor() }
            }
            KnowledgeIndexService().synchronize()
            val restoredChunkCount: Long = chunkCountFor()
            assertEquals(originalChunkCount, restoredChunkCount)
        }

    /** Chybějící zdrojový soubor vyhodí IllegalStateException */
    @Test
    public fun synchronizeFailsOnMissingDocument(): Unit =
        withB2bTestApp(TEST_DB_NAME) {
            prepareKnowledgeStore()
            val service = KnowledgeIndexService(listOf("warranty-rules.md", "neexistuje.md"))
            assertFailsWith<IllegalStateException> { service.synchronize() }
        }
}
