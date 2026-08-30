package cz.b2brental.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDate
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp

// Firmy
object Companies : LongIdTable("companies") {
    val name = varchar("name", 200)
    val inn = varchar("inn", 50)
    val address = varchar("address", 300)
    val creditLimit = decimal("credit_limit", 12, 2).default(50000.00.toBigDecimal())
    val verificationStatus = varchar("verification_status", 32).default("approved")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// Uživatelé
object Users : LongIdTable("users") {
    val companyId = reference("company_id", Companies).nullable()
    val email = varchar("email", 200).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val phone = varchar("phone", 50).nullable()
    val role = varchar("role", 32)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// Kategorie vybavení
object EquipmentCategories : LongIdTable("equipment_categories") {
    val name = varchar("name", 100).uniqueIndex()
    val icon = varchar("icon", 50)
}

// Vybavení
object Equipment : LongIdTable("equipment") {
    val categoryId = reference("category_id", EquipmentCategories)
    val model = varchar("model", 200)
    val serialNumber = varchar("serial_number", 100).uniqueIndex()
    val price = decimal("price", 12, 2)
    val monthlyRate = decimal("monthly_rate", 12, 2)
    val description = text("description").nullable()
    val photoUrl = text("photo_url").nullable()
    val status = enumerationByName<EquipmentStatus>("status", 32).default(EquipmentStatus.available)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// Nájemní smlouvy
object RentalContracts : LongIdTable("rental_contracts") {
    val companyId = reference("company_id", Companies)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val months = integer("months")
    val monthlyAmount = decimal("monthly_amount", 12, 2)
    val deposit = decimal("deposit", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val deliveryAddress = varchar("delivery_address", 300)
    val status = enumerationByName<ContractStatus>("status", 32).default(ContractStatus.draft)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// Položky smlouvy
object ContractItems : LongIdTable("contract_items") {
    val contractId = reference("contract_id", RentalContracts)
    val equipmentId = reference("equipment_id", Equipment)
}

// Platby
object Payments : LongIdTable("payments") {
    val contractId = reference("contract_id", RentalContracts)
    val period = integer("period")
    val amount = decimal("amount", 12, 2)
    val dueDate = date("due_date")
    val status = enumerationByName<PaymentStatus>("status", 32).default(PaymentStatus.unpaid)
    val paidAt = timestamp("paid_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// Servisní tikety
object ServiceTickets : LongIdTable("service_tickets") {
    val equipmentId = reference("equipment_id", Equipment)
    val companyId = reference("company_id", Companies)
    val description = text("description")
    val photoBase64 = text("photo_base64").nullable()
    val severity = enumerationByName<Severity>("severity", 32).nullable()
    val warrantyVerdict = enumerationByName<WarrantyVerdict>("warranty_verdict", 32).nullable()
    val warrantyReason = text("warranty_reason").nullable()
    val aiRecommendation = text("ai_recommendation").nullable()
    val technicianId = reference("technician_id", Users).nullable()
    val resolution = varchar("resolution", 32).nullable()
    val status = enumerationByName<TicketStatus>("status", 32).default(TicketStatus.new)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val resolvedAt = timestamp("resolved_at").nullable()
}

// Záruční pravidla
object WarrantyRules : LongIdTable("warranty_rules") {
    val categoryId = reference("category_id", EquipmentCategories).uniqueIndex()
    val warrantyMonths = integer("warranty_months")
    val coverage = varchar("coverage", 300)
    val excludedCauses = text("excluded_causes")
}

// Historie událostí
object HistoryEvents : LongIdTable("history_events") {
    val entityType = varchar("entity_type", 32)
    val entityId = long("entity_id")
    val eventType = varchar("event_type", 64)
    val message = text("message")
    val authorId = reference("author_id", Users).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// Dokumenty
object Documents : LongIdTable("documents") {
    val type = enumerationByName<DocumentType>("type", 32)
    val entityType = varchar("entity_type", 32)
    val entityId = long("entity_id")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val authorId = reference("author_id", Users).nullable()
}

// Notifikace
object Notifications : LongIdTable("notifications") {
    val userId = reference("user_id", Users)
    val message = text("message")
    val isRead = bool("is_read").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
