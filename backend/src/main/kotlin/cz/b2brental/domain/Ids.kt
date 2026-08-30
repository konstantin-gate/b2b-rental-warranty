package cz.b2brental.domain

import kotlinx.serialization.Serializable

/** Identifikátor společnosti */
@Serializable
@JvmInline
public value class CompanyId(
    public val value: Long,
)

/** Identifikátor uživatele */
@Serializable
@JvmInline
public value class UserId(
    public val value: Long,
)

/** Identifikátor kategorie vybavení */
@Serializable
@JvmInline
public value class EquipmentCategoryId(
    public val value: Long,
)

/** Identifikátor vybavení */
@Serializable
@JvmInline
public value class EquipmentId(
    public val value: Long,
)

/** Identifikátor nájemní smlouvy */
@Serializable
@JvmInline
public value class ContractId(
    public val value: Long,
)

/** Identifikátor položky nájemní smlouvy */
@Serializable
@JvmInline
public value class ContractItemId(
    public val value: Long,
)

/** Identifikátor platby */
@Serializable
@JvmInline
public value class PaymentId(
    public val value: Long,
)

/** Identifikátor servisního tiketu */
@Serializable
@JvmInline
public value class TicketId(
    public val value: Long,
)

/** Identifikátor záručního pravidla */
@Serializable
@JvmInline
public value class WarrantyRuleId(
    public val value: Long,
)

/** Identifikátor historické události */
@Serializable
@JvmInline
public value class HistoryEventId(
    public val value: Long,
)

/** Identifikátor dokumentu */
@Serializable
@JvmInline
public value class DocumentId(
    public val value: Long,
)

/** Identifikátor notifikace */
@Serializable
@JvmInline
public value class NotificationId(
    public val value: Long,
)
