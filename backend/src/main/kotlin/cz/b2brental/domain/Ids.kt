package cz.b2brental.domain

import kotlinx.serialization.Serializable

// Typované identifikátory entit (12 tabulek) — chrání před záměnou id různých entit
@Serializable
@JvmInline
value class CompanyId(val value: Long)

@Serializable
@JvmInline
value class UserId(val value: Long)

@Serializable
@JvmInline
value class EquipmentCategoryId(val value: Long)

@Serializable
@JvmInline
value class EquipmentId(val value: Long)

@Serializable
@JvmInline
value class ContractId(val value: Long)

@Serializable
@JvmInline
value class ContractItemId(val value: Long)

@Serializable
@JvmInline
value class PaymentId(val value: Long)

@Serializable
@JvmInline
value class TicketId(val value: Long)

@Serializable
@JvmInline
value class WarrantyRuleId(val value: Long)

@Serializable
@JvmInline
value class HistoryEventId(val value: Long)

@Serializable
@JvmInline
value class DocumentId(val value: Long)

@Serializable
@JvmInline
value class NotificationId(val value: Long)
