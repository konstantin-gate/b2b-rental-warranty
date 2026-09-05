@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental.models

import kotlinx.serialization.Serializable

/** Odpověď s údaji o technikovi pro přiřazení k tiketu */
@Serializable
public data class TechnicianResponse(
    public val id: Long,
    public val email: String,
    public val phone: String?,
)
