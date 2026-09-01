@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.db.DocumentType
import cz.b2brental.domain.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/** DTO pro přehled vygenerovaného dokumentu */
@Serializable
public data class DocumentResponse(
    public val id: Long,
    public val type: DocumentType,
    public val entityType: String,
    public val entityId: Long,
    @Serializable(with = InstantSerializer::class) public val createdAt: Instant,
)

/** DTO odpovědi s identifikátorem vygenerovaného PDF dokumentu */
@Serializable
public data class DocumentPdfResponse(
    public val documentId: Long,
)
