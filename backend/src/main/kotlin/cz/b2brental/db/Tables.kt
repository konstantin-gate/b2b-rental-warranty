@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.db

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Firmy */
public object Companies : LongIdTable("companies") {
    public val name: Column<String> = varchar("name", 200)
    public val inn: Column<String> = varchar("inn", 50)
    public val address: Column<String> = varchar("address", 300)
    public val creditLimit: Column<BigDecimal> = decimal("credit_limit", 12, 2).default(50000.00.toBigDecimal())
    public val verificationStatus: Column<String> = varchar("verification_status", 32).default("approved")
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Uživatelé */
public object Users : LongIdTable("users") {
    public val companyId: Column<EntityID<Long>?> = reference("company_id", Companies).nullable()
    public val email: Column<String> = varchar("email", 200).uniqueIndex()
    public val passwordHash: Column<String> = varchar("password_hash", 100)
    public val phone: Column<String?> = varchar("phone", 50).nullable()
    public val role: Column<String> = varchar("role", 32)
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Kategorie vybavení */
public object EquipmentCategories : LongIdTable("equipment_categories") {
    public val name: Column<String> = varchar("name", 100).uniqueIndex()
    public val icon: Column<String> = varchar("icon", 50)
}

/** Vybavení */
public object Equipment : LongIdTable("equipment") {
    public val categoryId: Column<EntityID<Long>> = reference("category_id", EquipmentCategories)
    public val model: Column<String> = varchar("model", 200)
    public val serialNumber: Column<String> = varchar("serial_number", 100).uniqueIndex()
    public val price: Column<BigDecimal> = decimal("price", 12, 2)
    public val monthlyRate: Column<BigDecimal> = decimal("monthly_rate", 12, 2)
    public val description: Column<String?> = text("description").nullable()
    public val photoUrl: Column<String?> = text("photo_url").nullable()
    public val status: Column<EquipmentStatus> = enumerationByName<EquipmentStatus>("status", 32).default(EquipmentStatus.available)
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Nájemní smlouvy */
public object RentalContracts : LongIdTable("rental_contracts") {
    public val companyId: Column<EntityID<Long>> = reference("company_id", Companies)
    public val startDate: Column<LocalDate> = date("start_date")
    public val endDate: Column<LocalDate> = date("end_date")
    public val months: Column<Int> = integer("months")
    public val monthlyAmount: Column<BigDecimal> = decimal("monthly_amount", 12, 2)
    public val deposit: Column<BigDecimal> = decimal("deposit", 12, 2)
    public val totalAmount: Column<BigDecimal> = decimal("total_amount", 12, 2)
    public val deliveryAddress: Column<String> = varchar("delivery_address", 300)
    public val status: Column<ContractStatus> = enumerationByName<ContractStatus>("status", 32).default(ContractStatus.draft)
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Položky smlouvy */
public object ContractItems : LongIdTable("contract_items") {
    public val contractId: Column<EntityID<Long>> = reference("contract_id", RentalContracts)
    public val equipmentId: Column<EntityID<Long>> = reference("equipment_id", Equipment)
}

/** Platby */
public object Payments : LongIdTable("payments") {
    public val contractId: Column<EntityID<Long>> = reference("contract_id", RentalContracts)
    public val period: Column<Int> = integer("period")
    public val amount: Column<BigDecimal> = decimal("amount", 12, 2)
    public val dueDate: Column<LocalDate> = date("due_date")
    public val status: Column<PaymentStatus> = enumerationByName<PaymentStatus>("status", 32).default(PaymentStatus.unpaid)
    public val paidAt: Column<Instant?> = timestamp("paid_at").nullable()
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Servisní tikety */
public object ServiceTickets : LongIdTable("service_tickets") {
    public val equipmentId: Column<EntityID<Long>> = reference("equipment_id", Equipment)
    public val companyId: Column<EntityID<Long>> = reference("company_id", Companies)
    public val description: Column<String> = text("description")
    public val photoBase64: Column<String?> = text("photo_base64").nullable()
    public val severity: Column<Severity?> = enumerationByName<Severity>("severity", 32).nullable()
    public val warrantyVerdict: Column<WarrantyVerdict?> = enumerationByName<WarrantyVerdict>("warranty_verdict", 32).nullable()
    public val warrantyReason: Column<String?> = text("warranty_reason").nullable()
    public val aiRecommendation: Column<String?> = text("ai_recommendation").nullable()
    public val technicianId: Column<EntityID<Long>?> = reference("technician_id", Users).nullable()
    public val resolution: Column<String?> = varchar("resolution", 32).nullable()
    public val status: Column<TicketStatus> = enumerationByName<TicketStatus>("status", 32).default(TicketStatus.new)
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
    public val resolvedAt: Column<Instant?> = timestamp("resolved_at").nullable()
}

/** Záruční pravidla */
public object WarrantyRules : LongIdTable("warranty_rules") {
    public val categoryId: Column<EntityID<Long>> = reference("category_id", EquipmentCategories).uniqueIndex()
    public val warrantyMonths: Column<Int> = integer("warranty_months")
    public val coverage: Column<String> = varchar("coverage", 300)
    public val excludedCauses: Column<String> = text("excluded_causes")
}

/** Historie událostí */
public object HistoryEvents : LongIdTable("history_events") {
    public val entityType: Column<String> = varchar("entity_type", 32)
    public val entityId: Column<Long> = long("entity_id")
    public val eventType: Column<String> = varchar("event_type", 64)
    public val message: Column<String> = text("message")
    public val authorId: Column<EntityID<Long>?> = reference("author_id", Users).nullable()
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Dokumenty */
public object Documents : LongIdTable("documents") {
    public val type: Column<DocumentType> = enumerationByName<DocumentType>("type", 32)
    public val entityType: Column<String> = varchar("entity_type", 32)
    public val entityId: Column<Long> = long("entity_id")
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
    public val authorId: Column<EntityID<Long>?> = reference("author_id", Users).nullable()
}

/** Notifikace */
public object Notifications : LongIdTable("notifications") {
    public val userId: Column<EntityID<Long>> = reference("user_id", Users)
    public val message: Column<String> = text("message")
    public val isRead: Column<Boolean> = bool("is_read").default(false)
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
