package cz.b2brental.db

// Stav vybavení
enum class EquipmentStatus {
    available,
    rented,
    maintenance
}

// Stav nájemní smlouvy
enum class ContractStatus {
    draft,
    pending,
    approved,
    rejected,
    active,
    completed,
    terminated
}

// Stav platby
enum class PaymentStatus {
    unpaid,
    paid,
    overdue
}

// Stav servisního tiketu
enum class TicketStatus {
    new,
    assigned,
    in_progress,
    resolved,
    rejected
}

// Závažnost problému
enum class Severity {
    low,
    medium,
    critical
}

// Verdikt záruky
enum class WarrantyVerdict {
    covered,
    not_covered,
    review_required
}

// Typ dokumentu
enum class DocumentType {
    rental_contract,
    acceptance_act,
    return_act,
    service_report,
    invoice
}
