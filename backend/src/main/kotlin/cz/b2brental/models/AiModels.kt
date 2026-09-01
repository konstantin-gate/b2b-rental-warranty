@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.db.Severity
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.domain.EquipmentId
import kotlinx.serialization.Serializable

/** Žádost na AI diagnostiku poruchy */
@Serializable
public data class DiagnoseRequest(
    public val description: String,
    public val photoBase64: String? = null,
)

/** Odpověď AI diagnostiky */
@Serializable
public data class DiagnoseResponse(
    public val possibleCause: String,
    public val severity: Severity,
    public val recommendation: String,
)

/** Žádost na kontrolu záručního krytí */
@Serializable
public data class WarrantyCheckRequest(
    public val equipmentId: EquipmentId,
    public val description: String? = null,
)

/** Odpověď kontroly záručního krytí */
@Serializable
public data class WarrantyCheckResponse(
    public val verdict: WarrantyVerdict,
    public val reason: String,
    public val aiExplanation: String,
)

/** Žádost na AI asistenta */
@Serializable
public data class AssistantRequest(
    public val message: String,
)

/** Odpověď AI asistenta */
@Serializable
public data class AssistantResponse(
    public val reply: String,
)
