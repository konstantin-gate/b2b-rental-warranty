@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Uživatelská role v systému B2B Rental. */
@Serializable
public enum class UserRole {
    /** Správce systému — plný přístup. */
    @SerialName("admin")
    ADMIN,

    /** Manažer — potvrzení smluv, přiřazení techniků, kontrola plateb. */
    @SerialName("manager")
    MANAGER,

    /** Technik — servisní tikety, diagnostika, opravy/výměny, akty. */
    @SerialName("technician")
    TECHNICIAN,

    /** Klient (restaurace / právnická osoba) — katalog, nájem, tikety, platby. */
    @SerialName("client")
    CLIENT,
}

/** Stav vybavení v katalogu. */
@Serializable
public enum class EquipmentStatus {
    /** Vybavení je dostupné pro nový pronájem. */
    @SerialName("available")
    AVAILABLE,

    /** Vybavení je aktuálně pronajato. */
    @SerialName("rented")
    RENTED,

    /** Vybavení je v údržbě (dočasně nedostupné). */
    @SerialName("maintenance")
    MAINTENANCE,
}

/** Stav nájemní smlouvy. */
@Serializable
public enum class ContractStatus {
    /** Koncept — vytvořen klientem, čeká na zpracování. */
    @SerialName("draft")
    DRAFT,

    /** Čeká na schválení manažerem. */
    @SerialName("pending")
    PENDING,

    /** Schváleno manažerem (před aktivací). */
    @SerialName("approved")
    APPROVED,

    /** Zamítnuto manažerem. */
    @SerialName("rejected")
    REJECTED,

    /** Aktivní smlouva (probíhá pronájem). */
    @SerialName("active")
    ACTIVE,

    /** Dokončeno (proběhl řádný návrat). */
    @SerialName("completed")
    COMPLETED,

    /** Předčasně ukončeno. */
    @SerialName("terminated")
    TERMINATED,
}

/** Stav platby. */
@Serializable
public enum class PaymentStatus {
    /** Nezaplaceno (v řádné lhůtě). */
    @SerialName("unpaid")
    UNPAID,

    /** Zaplaceno. */
    @SerialName("paid")
    PAID,

    /** Po splatnosti. */
    @SerialName("overdue")
    OVERDUE,
}

/** Stav servisního tiketu. */
@Serializable
public enum class TicketStatus {
    /** Nově vytvořený tiket. */
    @SerialName("new")
    NEW,

    /** Přiřazen technikovi. */
    @SerialName("assigned")
    ASSIGNED,

    /** Technik pracuje na tiketu. */
    @SerialName("in_progress")
    IN_PROGRESS,

    /** Tiket vyřešen. */
    @SerialName("resolved")
    RESOLVED,

    /** Tiket zamítnut (např. mimo záruku). */
    @SerialName("rejected")
    REJECTED,
}

/** Závažnost problému. */
@Serializable
public enum class Severity {
    /** Nízká závažnost. */
    @SerialName("low")
    LOW,

    /** Střední závažnost. */
    @SerialName("medium")
    MEDIUM,

    /** Kritická závažnost. */
    @SerialName("critical")
    CRITICAL,
}

/** Verdikt záruky. */
@Serializable
public enum class WarrantyVerdict {
    /** Porucha je kryta zárukou. */
    @SerialName("covered")
    COVERED,

    /** Porucha není kryta zárukou. */
    @SerialName("not_covered")
    NOT_COVERED,

    /** Vyžaduje ruční posouzení. */
    @SerialName("review_required")
    REVIEW_REQUIRED,
}

/** Typ dokumentu. */
@Serializable
public enum class DocumentType {
    /** Nájemní smlouva. */
    @SerialName("rental_contract")
    RENTAL_CONTRACT,

    /** Akt předání / vstupní akt. */
    @SerialName("acceptance_act")
    ACCEPTANCE_ACT,

    /** Akt vrácení. */
    @SerialName("return_act")
    RETURN_ACT,

    /** Servisní zpráva / protokol o opravě. */
    @SerialName("service_report")
    SERVICE_REPORT,

    /** Faktura. */
    @SerialName("invoice")
    INVOICE,
}

/** Lokální výběr výsledku řešení tiketu technikem. */
public enum class Resolution {
    /** Technik provedl opravu. */
    REPAIRED,

    /** Technik provedl výměnu. */
    REPLACED,

    /** Porucha není kryta zárukou. */
    NOT_COVERED,
}

/** Mapování výsledku řešení na wire-řetězec backend API. */
public fun Resolution.toWire(): String = when (this) {
    Resolution.REPAIRED -> "repaired"
    Resolution.REPLACED -> "replaced"
    Resolution.NOT_COVERED -> "not_covered"
}
