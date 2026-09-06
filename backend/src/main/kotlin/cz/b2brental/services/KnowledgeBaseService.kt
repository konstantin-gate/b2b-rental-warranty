@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.KnowledgeChunks
import cz.b2brental.db.KnowledgeDocuments
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Dotaz na znalostní bázi.
 * @property text volný text dotazu (pro fulltextové skóre)
 * @property categoryName volitelný filtr podle kategorie vybavení
 * @property equipmentModel volitelný filtr podle modelu vybavení
 * @property errorCode volitelný filtr podle kódu chyby
 */
public data class KnowledgeQuery(
    public val text: String,
    public val categoryName: String?,
    public val equipmentModel: String?,
    public val errorCode: String?,
)

/**
 * Jeden výsledek (úryvek) z znalostní báze.
 * @property citation citace ve formátu [KB:<slug>#<ordinal>]
 * @property heading nadpis sekce znalostního dokumentu
 * @property content tělo sekce znalostního dokumentu
 * @property score výsledné skóre relevace (body podle pravidel retrievalu)
 */
public data class KnowledgeSnippet(
    public val citation: String,
    public val heading: String,
    public val content: String,
    public val score: Int,
)

/**
 * Kontext získaný z znalostní báze pro prompt jazykového modelu.
 * @property snippets seznam úryvků seřazený podle relevance
 */
public data class KnowledgeContext(
    public val snippets: List<KnowledgeSnippet>,
) {
    /**
     * Vrátí kontext jako textový blok pro připojení k promptu.
     * Prázdný kontext dává prázdný řetězec, jinak řádky ve tvaru
     * [KB:<slug>#<ordinal>] <nadpis>: <obsah> spojené novým řádkem.
     * @return textový blok kontextu nebo prázdný řetězec
     */
    @Suppress("KDocMissingDocumentation")
    public fun asPromptBlock(): String =
        if (snippets.isEmpty()) {
            ""
        } else {
            snippets.joinToString(separator = "\n") { snippet ->
                "${snippet.citation} ${snippet.heading}: ${snippet.content}"
            }
        }
}

/**
 * Vyhledávací služba nad znalostní bází (RAG retrieval).
 * Skóruje chunky podle přesných shod meta údajů a překryvu fulltextových tokenů,
 * vrací nejlepších 5 úryvků s celkovým limitem obsahu 6000 znaků.
 */
public class KnowledgeBaseService {
    /**
     * Vyhledá relevantní úryvky v aktivních znalostních dokumentech ve vlastní transakci.
     * Bez shod vrací prázdný [KnowledgeContext]; žádný dokument se náhodou nepodstrkává.
     * @param query dotaz s volným textem a volitelnými filtry
     * @return kontext s seřazenými úryvky
     */
    @Suppress("KDocMissingDocumentation")
    public fun retrieve(query: KnowledgeQuery): KnowledgeContext =
        transaction {
            val rows: List<ResultRow> =
                (KnowledgeChunks innerJoin KnowledgeDocuments)
                    .selectAll()
                    .where { KnowledgeDocuments.active eq true }
                    .toList()
            val queryTokens: List<String> =
                normalizeSearchText(query.text).split(' ').filter { token -> token.length >= MIN_TOKEN_LENGTH }
            val scored: List<ScoredChunk> =
                rows.mapNotNull { row -> scoreChunk(row, query, queryTokens) }
            val comparator: Comparator<ScoredChunk> =
                compareByDescending<ScoredChunk> { chunk -> chunk.score }
                    .thenBy { chunk -> chunk.documentId }
                    .thenBy { chunk -> chunk.ordinal }
            val ordered: List<ScoredChunk> = scored.sortedWith(comparator).take(MAX_SNIPPETS)
            var remaining: Int = MAX_TOTAL_CONTENT_LENGTH
            val snippets: MutableList<KnowledgeSnippet> = mutableListOf()
            for (chunk in ordered) {
                if (remaining <= 0) break
                val content: String =
                    if (chunk.content.length > remaining) {
                        chunk.content.take(remaining)
                    } else {
                        chunk.content
                    }
                remaining -= content.length
                snippets.add(
                    KnowledgeSnippet(
                        citation = chunk.citation,
                        heading = chunk.heading,
                        content = content,
                        score = chunk.score,
                    ),
                )
            }
            KnowledgeContext(snippets)
        }

    /**
     * Vypočítá skóre jednoho chunku podle pravidel retrievalu:
     * přesná shoda error_code +100, shoda modelu +60, shoda kategorie +30,
     * +1 za každý token dotazu (délka alespoň [MIN_TOKEN_LENGTH]) přítomný v searchText,
     * nejvýše +10. Chunk bez bodů se zahodí.
     * @param row řádek spojení chunku a dokumentu
     * @param query dotaz
     * @param queryTokens normalizované tokeny dotazu
     * @return scoreovaný chunk nebo null bez bodů
     */
    @Suppress("KDocMissingDocumentation")
    private fun scoreChunk(
        row: ResultRow,
        query: KnowledgeQuery,
        queryTokens: List<String>,
    ): ScoredChunk? {
        var score: Int = 0
        val chunkErrorCode: String? = row[KnowledgeChunks.errorCode]
        if (!query.errorCode.isNullOrEmpty() &&
            chunkErrorCode != null &&
            chunkErrorCode.equals(query.errorCode, ignoreCase = true)
        ) {
            score += ERROR_CODE_POINTS
        }
        val chunkEquipmentModel: String? = row[KnowledgeChunks.equipmentModel]
        if (!query.equipmentModel.isNullOrEmpty() &&
            chunkEquipmentModel != null &&
            chunkEquipmentModel.equals(query.equipmentModel, ignoreCase = true)
        ) {
            score += EQUIPMENT_MODEL_POINTS
        }
        val chunkCategoryName: String? = row[KnowledgeChunks.categoryName]
        if (!query.categoryName.isNullOrEmpty() &&
            chunkCategoryName != null &&
            chunkCategoryName.equals(query.categoryName, ignoreCase = true)
        ) {
            score += CATEGORY_NAME_POINTS
        }
        val chunkTokens: Set<String> =
            row[KnowledgeChunks.searchText].split(' ').filter { token -> token.isNotEmpty() }.toHashSet()
        var wordPoints: Int = 0
        for (token in queryTokens) {
            if (token in chunkTokens) {
                wordPoints++
                if (wordPoints >= MAX_WORD_POINTS) break
            }
        }
        score += wordPoints
        if (score == 0) return null
        val documentId: Long = row[KnowledgeDocuments.id].value
        val ordinal: Int = row[KnowledgeChunks.ordinal]
        return ScoredChunk(
            citation = "[KB:${row[KnowledgeDocuments.slug]}#$ordinal]",
            heading = row[KnowledgeChunks.heading],
            content = row[KnowledgeChunks.content],
            score = score,
            documentId = documentId,
            ordinal = ordinal,
        )
    }

    /**
     * Vnitřní reprezentace scoreovaného chunku před vytvořením [KnowledgeSnippet].
     * @property citation citace chunku
     * @property heading nadpis sekce
     * @property content tělo sekce
     * @property score skóre relevace
     * @property documentId identifikátor dokumentu (pro stabilní řazení)
     * @property ordinal pořadí sekce v dokumentu (pro stabilní řazení)
     */
    private data class ScoredChunk(
        val citation: String,
        val heading: String,
        val content: String,
        val score: Int,
        val documentId: Long,
        val ordinal: Int,
    )

    private companion object {
        /** Body za přesnou shodu error_code */
        public const val ERROR_CODE_POINTS: Int = 100

        /** Body za shodu modelu vybavení */
        public const val EQUIPMENT_MODEL_POINTS: Int = 60

        /** Body za shodu kategorie vybavení */
        public const val CATEGORY_NAME_POINTS: Int = 30

        /** Maximální body za překryv tokenů */
        public const val MAX_WORD_POINTS: Int = 10

        /** Minimální délka tokenu pro fulltextové skóre */
        public const val MIN_TOKEN_LENGTH: Int = 4

        /** Maximální počet úryvků v odpovědi */
        public const val MAX_SNIPPETS: Int = 5

        /** Maximální celková délka obsahu úryvků */
        public const val MAX_TOTAL_CONTENT_LENGTH: Int = 6000
    }
}
