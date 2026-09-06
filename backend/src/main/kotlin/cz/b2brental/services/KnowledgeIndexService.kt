@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.KnowledgeChunks
import cz.b2brental.db.KnowledgeDocuments
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Instant

/**
 * Normalizuje text pro fulltextové porovnávání: malá písmena, odstranění diakritiky
 * a nahrazení všeho mimo písmena a číslice mezerami.
 * @param raw vstupní text k normalizaci
 * @return normalizovaný text vhodný pro vyhledávání
 */
public fun normalizeSearchText(raw: String): String {
    val decomposed: String = Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFKD)
    val withoutMarks: String = Regex("\\p{M}+").replace(decomposed, "")
    val normalized: String = Regex("[^\\p{L}\\p{Nd}]+").replace(withoutMarks, " ")
    return normalized.trim().replace(Regex("\\s+"), " ")
}

/**
 * Jedna sekce znalostního dokumentu získaná z Markdown souboru.
 * @property heading nadpis sekce (text za "## ")
 * @property content tělo sekce bez řádku meta:, trimované
 * @property categoryName volitelná kategorie z řádku meta:
 * @property equipmentModel volitelný model vybavení z řádku meta:
 * @property errorCode volitelný kód chyby z řádku meta:
 */
private data class ParsedSection(
    val heading: String,
    val content: String,
    val categoryName: String?,
    val equipmentModel: String?,
    val errorCode: String?,
)

/**
 * Znalostní dokument získaný z Markdown souboru.
 * @property slug jednoznačný identifikátor dokumentu
 * @property title název dokumentu
 * @property language jazyk dokumentu
 * @property sourcePath cesta ke zdrojovému souboru
 * @property sourceSha256 kontrolní součet SHA-256 surových bajtů souboru (hex, malá písmena)
 * @property sections sekce dokumentu v pořadí výskytu
 */
private data class ParsedDocument(
    val slug: String,
    val title: String,
    val language: String,
    val sourcePath: String,
    val sourceSha256: String,
    val sections: List<ParsedSection>,
)

/**
 * Indexátor znalostní báze: čte Markdown soubory z resources/knowledge/, parsuje je
 * a idempotentně synchronizuje jejich obsah do tabulek knowledge_documents a knowledge_chunks.
 * @property documentNames názvy souborů ke zpracování (výchozí [DEFAULT_DOCUMENT_NAMES])
 */
public class KnowledgeIndexService(
    private val documentNames: List<String> = DEFAULT_DOCUMENT_NAMES,
) {
    /**
     * Synchronizuje znalostní dokumenty s databází ve vlastní transakci.
     * Dokument se stejným SHA-256 a aktivním stavem se nechá beze změny,
     * změněný dokument se upsertne a jeho chunky se přegenerují,
     * cizí (již neexistující) dokumenty se deaktivují. Chyba čtení nebo parsování
     * vyhazuje [IllegalStateException] a přeruší tak start backendu.
     */
    @Suppress("KDocMissingDocumentation")
    public fun synchronize() {
        val documents: List<ParsedDocument> =
            documentNames.map { name ->
                val bytes: ByteArray = readResource(name)
                val text: String = String(bytes, Charsets.UTF_8)
                parseDocument(name, text, bytes)
            }
        transaction {
            val processedSlugs = mutableListOf<String>()
            for (document in documents) {
                processedSlugs.add(document.slug)
                val existing =
                    KnowledgeDocuments
                        .selectAll()
                        .where { KnowledgeDocuments.slug eq document.slug }
                        .singleOrNull()
                if (existing != null &&
                    existing[KnowledgeDocuments.sourceSha256] == document.sourceSha256 &&
                    existing[KnowledgeDocuments.active]
                ) {
                    continue
                }
                val docId: EntityID<Long> =
                    if (existing != null) {
                        KnowledgeDocuments.update({ KnowledgeDocuments.slug eq document.slug }) { row ->
                            row[title] = document.title
                            row[sourcePath] = document.sourcePath
                            row[sourceSha256] = document.sourceSha256
                            row[language] = document.language
                            row[active] = true
                            row[updatedAt] = Instant.now()
                        }
                        existing[KnowledgeDocuments.id]
                    } else {
                        KnowledgeDocuments.insert { row ->
                            row[slug] = document.slug
                            row[title] = document.title
                            row[sourcePath] = document.sourcePath
                            row[sourceSha256] = document.sourceSha256
                            row[language] = document.language
                            row[active] = true
                        } get KnowledgeDocuments.id
                    }
                KnowledgeChunks.deleteWhere { KnowledgeChunks.documentId eq docId }
                document.sections.forEachIndexed { index, section ->
                    KnowledgeChunks.insert { row ->
                        row[documentId] = docId
                        row[ordinal] = index + 1
                        row[categoryName] = section.categoryName
                        row[equipmentModel] = section.equipmentModel
                        row[errorCode] = section.errorCode
                        row[heading] = section.heading
                        row[content] = section.content
                        row[searchText] = normalizeSearchText(section.heading + " " + section.content)
                    }
                }
            }
            KnowledgeDocuments.update({ KnowledgeDocuments.slug notInList processedSlugs }) { row ->
                row[active] = false
            }
        }
    }

    /** Výchozí seznam znalostních souborů ve složce resources/knowledge/ */
    public companion object {
        /** Názvy povinných znalostních souborů (řazení určuje i pořadí dokumentů v bázi) */
        public val DEFAULT_DOCUMENT_NAMES: List<String> =
            listOf("warranty-rules.md", "ticket-workflow.md", "safe-diagnostics.md", "manager-assistant.md")
    }

    /**
     * Přečte znalostní soubor z resources.
     * @param name název souboru ve složce knowledge/
     * @return surové bajty souboru
     * @throws IllegalStateException pokud zdroj neexistuje
     */
    @Suppress("KDocMissingDocumentation")
    private fun readResource(name: String): ByteArray {
        val stream =
            javaClass.classLoader.getResourceAsStream("knowledge/$name")
                ?: throw IllegalStateException("Chybí povinný zdroj znalostí: knowledge/$name")
        return stream.use { input -> input.readBytes() }
    }

    /**
     * Naparsuje Markdown dokument: front matter a sekce "## ".
     * @param name název souboru (pro chybové zprávy)
     * @param rawText text souboru
     * @param rawBytes surové bajty souboru (pro výpočet SHA-256)
     * @return naparsovaný dokument
     * @throws IllegalStateException při nevalidní struktuře dokumentu
     */
    @Suppress("KDocMissingDocumentation")
    private fun parseDocument(
        name: String,
        rawText: String,
        rawBytes: ByteArray,
    ): ParsedDocument {
        val lines: List<String> = rawText.lines()
        if (lines.isEmpty() || lines.first().trim() != "---") {
            throw IllegalStateException("Dokument knowledge/$name nezačíná oddělovačem front matter: ---")
        }
        val metadata = mutableMapOf<String, String>()
        var index = 1
        while (index < lines.size && lines[index].trim() != "---") {
            val line: String = lines[index].trim()
            if (line.isNotEmpty()) {
                val separator = line.indexOf(':')
                if (separator <= 0) {
                    throw IllegalStateException("Neplatný řádek front matter v dokumentu knowledge/$name: $line")
                }
                metadata[line.substring(0, separator).trim()] = line.substring(separator + 1).trim()
            }
            index++
        }
        if (index >= lines.size) {
            throw IllegalStateException("Dokument knowledge/$name nemá ukončovací oddělovač front matter: ---")
        }
        val slug: String =
            metadata["slug"]
                ?: throw IllegalStateException("Chybí povinný klíč slug ve front matter dokumentu knowledge/$name")
        val title: String =
            metadata["title"]
                ?: throw IllegalStateException("Chybí povinný klíč title ve front matter dokumentu knowledge/$name")
        val language: String =
            metadata["language"]
                ?: throw IllegalStateException("Chybí povinný klíč language ve front matter dokumentu knowledge/$name")

        var heading: String? = null
        val contentLines = mutableListOf<String>()
        val sections = mutableListOf<ParsedSection>()

        fun flushSection() {
            val currentHeading: String = heading ?: return
            val rawBody: String = contentLines.joinToString("\n").trim()
            if (rawBody.isEmpty()) {
                throw IllegalStateException("Sekce '$currentHeading' dokumentu knowledge/$name nemá žádné tělo")
            }
            var body: String = rawBody
            var categoryName: String? = null
            var equipmentModel: String? = null
            var errorCode: String? = null
            val firstNewline = body.indexOf('\n')
            val firstLine: String = if (firstNewline >= 0) body.substring(0, firstNewline) else body
            if (firstLine.trimStart().startsWith("meta:")) {
                val metaBody: String = firstLine.trim().removePrefix("meta:").trim()
                for (pair in metaBody.split(';')) {
                    val item: String = pair.trim()
                    if (item.isEmpty()) continue
                    val separator = item.indexOf('=')
                    if (separator <= 0) {
                        throw IllegalStateException(
                            "Neplatná položka meta: '$item' v sekci '$currentHeading' dokumentu knowledge/$name",
                        )
                    }
                    val key: String = item.substring(0, separator).trim()
                    val value: String = item.substring(separator + 1).trim()
                    when (key) {
                        "category" -> categoryName = value
                        "model" -> equipmentModel = value
                        "error_code" -> errorCode = value
                        else -> throw IllegalStateException(
                            "Neznámý klíč meta: '$key' v sekci '$currentHeading' dokumentu knowledge/$name",
                        )
                    }
                }
                body = if (firstNewline >= 0) body.substring(firstNewline + 1).trim() else ""
                if (body.isEmpty()) {
                    throw IllegalStateException(
                        "Sekce '$currentHeading' dokumentu knowledge/$name nemá po řádku meta: žádné tělo",
                    )
                }
            }
            sections.add(ParsedSection(currentHeading, body, categoryName, equipmentModel, errorCode))
        }

        for (line in lines.subList(index + 1, lines.size)) {
            if (line.startsWith("## ")) {
                flushSection()
                heading = line.removePrefix("## ").trim()
                contentLines.clear()
            } else if (heading != null) {
                contentLines.add(line)
            }
        }
        flushSection()
        if (sections.isEmpty()) {
            throw IllegalStateException("Dokument knowledge/$name neobsahuje žádné sekce s nadpisem '## '")
        }
        return ParsedDocument(
            slug = slug,
            title = title,
            language = language,
            sourcePath = "knowledge/$name",
            sourceSha256 = sha256Hex(rawBytes),
            sections = sections.toList(),
        )
    }

    /**
     * Vypočítá SHA-256 vstupních bajtů jako hex řetězec s malými písmeny.
     * @param bytes vstupní bajty
     * @return hex podpis SHA-256
     */
    @Suppress("KDocMissingDocumentation")
    private fun sha256Hex(bytes: ByteArray): String {
        val digest: ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
