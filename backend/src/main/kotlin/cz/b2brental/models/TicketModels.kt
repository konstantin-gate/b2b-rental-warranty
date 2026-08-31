@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.db.Severity
import cz.b2brental.db.TicketStatus
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.domain.EquipmentId
import cz.b2brental.domain.InstantSerializer
import cz.b2brental.domain.UserId
import kotlinx.serialization.Serializable
import java.time.Instant

/** Žádost o vytvoření servisního tiketu */
@Serializable
public data class TicketCreateRequest(
    public val equipmentId: EquipmentId,
    public val description: String,
    public val photoBase64: String? = null,
)

/** Žádost o přiřazení technika k tiketu */
@Serializable
public data class AssignRequest(
    public val technicianId: UserId,
)

/** Žádost o vyřešení tiketu */
@Serializable
public data class ResolveRequest(
    public val result: String,
    public val notes: String,
)

/** Odpověď s údaji o servisním tiketu */
@Serializable
public data class TicketResponse(
    public val id: Long,
    public val equipmentId: Long,
    public val equipmentModel: String,
    public val companyName: String,
    public val description: String,
    public val photoBase64: String?,
    public val severity: Severity?,
    public val status: TicketStatus,
    public val warrantyVerdict: WarrantyVerdict?,
    public val warrantyReason: String?,
    public val aiRecommendation: String?,
    public val technicianId: Long?,
    public val resolution: String?,
    @Serializable(with = InstantSerializer::class) public val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) public val resolvedAt: Instant?,
)

/** Odpověď po provedení akce s tiketem */
@Serializable
public data class TicketActionResponse(
    public val id: Long,
    public val status: TicketStatus,
    public val reportDocumentId: Long? = null,
)
