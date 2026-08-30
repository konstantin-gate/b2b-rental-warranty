@file:Suppress("ktlint:standard:enum-entry-name-case")

package cz.b2brental.db

/** Stav vybavení */
public enum class EquipmentStatus {
    available,
    rented,
    maintenance,
}

/** Stav nájemní smlouvy */
public enum class ContractStatus {
    draft,
    pending,
    approved,
    rejected,
    active,
    completed,
    terminated,
}

/** Stav platby */
public enum class PaymentStatus {
    unpaid,
    paid,
    overdue,
}

/** Stav servisního tiketu */
public enum class TicketStatus {
    new,
    assigned,
    in_progress,
    resolved,
    rejected,
}

/** Závažnost problému */
public enum class Severity {
    low,
    medium,
    critical,
}

/** Verdikt záruky */
public enum class WarrantyVerdict {
    covered,
    not_covered,
    review_required,
}

/** Typ dokumentu */
public enum class DocumentType {
    rental_contract,
    acceptance_act,
    return_act,
    service_report,
    invoice,
}
