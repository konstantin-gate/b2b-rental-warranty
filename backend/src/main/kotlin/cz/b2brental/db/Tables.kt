@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.db

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Firmy */
public object Companies : LongIdTable("companies") {
    /** Název firmy */
    public val name: Column<String> = varchar("name", 200)

    /** Identifikační číslo (INN) firmy */
    public val inn: Column<String> = varchar("inn", 50)

    /** Sídlo firmy */
    public val address: Column<String> = varchar("address", 300)

    /** Úvěrový limit pro nájemní smlouvy */
    public val creditLimit: Column<BigDecimal> = decimal("credit_limit", 12, 2).default(50000.00.toBigDecimal())

    /** Stav ověření firmy (demo: vždy approved) */
    public val verificationStatus: Column<String> = varchar("verification_status", 32).default("approved")

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Uživatelé */
public object Users : LongIdTable("users") {
    /** Vlastnická firma uživatele (null u administrátorů systému) */
    public val companyId: Column<EntityID<Long>?> = reference("company_id", Companies).nullable()

    /** Přihlašovací e-mail, jednoznačný */
    public val email: Column<String> = varchar("email", 200).uniqueIndex()

    /** Bcrypt hash hesla */
    public val passwordHash: Column<String> = varchar("password_hash", 100)

    /** Volitelný telefon */
    public val phone: Column<String?> = varchar("phone", 50).nullable()

    /** Role uživatele (admin, manager, technician, client) */
    public val role: Column<String> = varchar("role", 32)

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Kategorie vybavení */
public object EquipmentCategories : LongIdTable("equipment_categories") {
    /** Název kategorie, jednoznačný */
    public val name: Column<String> = varchar("name", 100).uniqueIndex()

    /** Ikona kategorie pro UI */
    public val icon: Column<String> = varchar("icon", 50)
}

/** Vybavení */
public object Equipment : LongIdTable("equipment") {
    /** Kategorie vybavení */
    public val categoryId: Column<EntityID<Long>> = reference("category_id", EquipmentCategories)

    /** Model vybavení */
    public val model: Column<String> = varchar("model", 200)

    /** Sériové číslo, jednoznačné */
    public val serialNumber: Column<String> = varchar("serial_number", 100).uniqueIndex()

    /** Prodejní cena vybavení */
    public val price: Column<BigDecimal> = decimal("price", 12, 2)

    /** Měsíční nájemná sazba */
    public val monthlyRate: Column<BigDecimal> = decimal("monthly_rate", 12, 2)

    /** Volitelný popis vybavení */
    public val description: Column<String?> = text("description").nullable()

    /** Volitelná adresa fotografie vybavení */
    public val photoUrl: Column<String?> = text("photo_url").nullable()

    /** Stav vybavení v životním cyklu */
    public val status: Column<EquipmentStatus> = enumerationByName<EquipmentStatus>("status", 32).default(EquipmentStatus.available)

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Nájemní smlouvy */
public object RentalContracts : LongIdTable("rental_contracts") {
    /** Firma, se kterou je smlouva uzavřena */
    public val companyId: Column<EntityID<Long>> = reference("company_id", Companies)

    /** Datum začátku smlouvy */
    public val startDate: Column<LocalDate> = date("start_date")

    /** Datum konce smlouvy */
    public val endDate: Column<LocalDate> = date("end_date")

    /** Doba smlouvy v měsících */
    public val months: Column<Int> = integer("months")

    /** Měsíční částka nájmu */
    public val monthlyAmount: Column<BigDecimal> = decimal("monthly_amount", 12, 2)

    /** Vratná kauce */
    public val deposit: Column<BigDecimal> = decimal("deposit", 12, 2)

    /** Celková částka za celé období smlouvy */
    public val totalAmount: Column<BigDecimal> = decimal("total_amount", 12, 2)

    /** Adresa dodání a instalace vybavení */
    public val deliveryAddress: Column<String> = varchar("delivery_address", 300)

    /** Stav smlouvy v životním cyklu */
    public val status: Column<ContractStatus> = enumerationByName<ContractStatus>("status", 32).default(ContractStatus.draft)

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Položky smlouvy */
public object ContractItems : LongIdTable("contract_items") {
    /** Smlouva, do níž položka patří */
    public val contractId: Column<EntityID<Long>> = reference("contract_id", RentalContracts)

    /** Vybavení zařazené do smlouvy */
    public val equipmentId: Column<EntityID<Long>> = reference("equipment_id", Equipment)
}

/** Platby */
public object Payments : LongIdTable("payments") {
    /** Smlouva, ke které platba náleží */
    public val contractId: Column<EntityID<Long>> = reference("contract_id", RentalContracts)

    /** Pořadové číslo platebního období (1 = první měsíc) */
    public val period: Column<Int> = integer("period")

    /** Částka platby */
    public val amount: Column<BigDecimal> = decimal("amount", 12, 2)

    /** Datum splatnosti */
    public val dueDate: Column<LocalDate> = date("due_date")

    /** Stav platby */
    public val status: Column<PaymentStatus> = enumerationByName<PaymentStatus>("status", 32).default(PaymentStatus.unpaid)

    /** Čas úspěšného zaplacení (null u nezaplacených) */
    public val paidAt: Column<Instant?> = timestamp("paid_at").nullable()

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Servisní tikety */
public object ServiceTickets : LongIdTable("service_tickets") {
    /** Vybavení, kterého se porucha týká */
    public val equipmentId: Column<EntityID<Long>> = reference("equipment_id", Equipment)

    /** Firma, která poruchu nahlásila */
    public val companyId: Column<EntityID<Long>> = reference("company_id", Companies)

    /** Textový popis poruchy od klienta */
    public val description: Column<String> = text("description")

    /** Fotografie poruchy zakódovaná v Base64 (volitelná) */
    public val photoBase64: Column<String?> = text("photo_base64").nullable()

    /** Závažnost poruchy stanovená AI (volitelná) */
    public val severity: Column<Severity?> = enumerationByName<Severity>("severity", 32).nullable()

    /** Verdikt pokrytí zárukou (volitelný) */
    public val warrantyVerdict: Column<WarrantyVerdict?> = enumerationByName<WarrantyVerdict>("warranty_verdict", 32).nullable()

    /** Zdůvodnění verdiktu záruky (volitelné) */
    public val warrantyReason: Column<String?> = text("warranty_reason").nullable()

    /** Doporučení AI pro technika (volitelné) */
    public val aiRecommendation: Column<String?> = text("ai_recommendation").nullable()

    /** Technik přiřazený k řešení (volitelný) */
    public val technicianId: Column<EntityID<Long>?> = reference("technician_id", Users).nullable()

    /** Popis provedeného řešení (volitelný) */
    public val resolution: Column<String?> = varchar("resolution", 32).nullable()

    /** Stav tiketu v životním cyklu */
    public val status: Column<TicketStatus> = enumerationByName<TicketStatus>("status", 32).default(TicketStatus.new)

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)

    /** Čas vyřešení tiketu (volitelný) */
    public val resolvedAt: Column<Instant?> = timestamp("resolved_at").nullable()
}

/** Záruční pravidla */
public object WarrantyRules : LongIdTable("warranty_rules") {
    /** Kategorie vybavení, pro kterou pravidlo platí (jednoznačné) */
    public val categoryId: Column<EntityID<Long>> = reference("category_id", EquipmentCategories).uniqueIndex()

    /** Délka záruky v měsících */
    public val warrantyMonths: Column<Int> = integer("warranty_months")

    /** Popis pokrytí zárukou */
    public val coverage: Column<String> = varchar("coverage", 300)

    /** Textový seznam vyjmutých příčin poruch */
    public val excludedCauses: Column<String> = text("excluded_causes")
}

/** Historie událostí */
public object HistoryEvents : LongIdTable("history_events") {
    /** Typ entity (equipment, contract, ticket) */
    public val entityType: Column<String> = varchar("entity_type", 32)

    /** Identifikátor entity */
    public val entityId: Column<Long> = long("entity_id")

    /** Typ události v životním cyklu */
    public val eventType: Column<String> = varchar("event_type", 64)

    /** Text zprávy události */
    public val message: Column<String> = text("message")

    /** Autor události (volitelný u systémových událostí) */
    public val authorId: Column<EntityID<Long>?> = reference("author_id", Users).nullable()

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Dokumenty */
public object Documents : LongIdTable("documents") {
    /** Typ dokumentu (smlouva, akt, report) */
    public val type: Column<DocumentType> = enumerationByName<DocumentType>("type", 32)

    /** Typ navázané entity */
    public val entityType: Column<String> = varchar("entity_type", 32)

    /** Identifikátor navázané entity */
    public val entityId: Column<Long> = long("entity_id")

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)

    /** Autor dokumentu (volitelný) */
    public val authorId: Column<EntityID<Long>?> = reference("author_id", Users).nullable()
}

/** Notifikace */
public object Notifications : LongIdTable("notifications") {
    /** Uživatel, kterému notifikace náleží */
    public val userId: Column<EntityID<Long>> = reference("user_id", Users)

    /** Text notifikace */
    public val message: Column<String> = text("message")

    /** Příznak přečtení */
    public val isRead: Column<Boolean> = bool("is_read").default(false)

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** Znalostní dokumenty (RAG) */
public object KnowledgeDocuments : LongIdTable("knowledge_documents") {
    /** Jednoznačný identifikátor dokumentu */
    public val slug: Column<String> = varchar("slug", 160).uniqueIndex()

    /** Název dokumentu */
    public val title: Column<String> = varchar("title", 240)

    /** Cesta ke zdrojovému souboru v resources */
    public val sourcePath: Column<String> = varchar("source_path", 500)

    /** SHA-256 surových bajtů zdrojového souboru (hex) */
    public val sourceSha256: Column<String> = varchar("source_sha256", 64)

    /** Jazyk dokumentu (kód dle ISO 639-1) */
    public val language: Column<String> = varchar("language", 8)

    /** Příznak aktivity dokumentu pro retrieval */
    public val active: Column<Boolean> = bool("active")

    /** Čas vytvoření záznamu */
    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)

    /** Čas poslední synchronizace */
    public val updatedAt: Column<Instant> = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

/** Části (chunky) znalostních dokumentů (RAG) */
public object KnowledgeChunks : LongIdTable("knowledge_chunks") {
    /** Rodičovský dokument (mazání kaskádové) */
    public val documentId: Column<EntityID<Long>> =
        reference("document_id", KnowledgeDocuments, onDelete = ReferenceOption.CASCADE)

    /** Pořadí sekce v dokumentu (od 1) */
    public val ordinal: Column<Int> = integer("ordinal")

    /** Volitelný filtr podle kategorie vybavení */
    public val categoryName: Column<String?> = varchar("category_name", 100).nullable()

    /** Volitelný filtr podle modelu vybavení */
    public val equipmentModel: Column<String?> = varchar("equipment_model", 200).nullable()

    /** Volitelný filtr podle kódu chyby */
    public val errorCode: Column<String?> = varchar("error_code", 80).nullable()

    /** Nadpis sekce */
    public val heading: Column<String> = varchar("heading", 240)

    /** Tělo sekce */
    public val content: Column<String> = text("content")

    /** Normalizovaný text pro fulltextové vyhledávání */
    public val searchText: Column<String> = text("search_text")

    init {
        uniqueIndex("uq_knowledge_chunks_document_ordinal", documentId, ordinal)
    }
}
