@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.presentation.navigation

/**
 * Konstantní definice navigačních tras aplikace B2B Rental.
 */
public object Routes {
    /** Přihlašovací obrazovka. */
    public const val LOGIN: String = "login"

    /** Registrační obrazovka firmy. */
    public const val REGISTER: String = "register"

    /** Katalog vybavení (seznam). */
    public const val CATALOG: String = "catalog"

    /** Detail vybavení (parametr: equipmentId). */
    public const val EQUIPMENT_DETAIL: String = "equipment/{equipmentId}"

    /** Obrazovka vytvoření nájemní smlouvy (parametr: ids — seznam ID vybavení oddělený čárkou). */
    public const val CONTRACT_NEW: String = "contract-new/{ids}"

    /** Seznam nájemních smluv. */
    public const val CONTRACTS: String = "contracts"

    /** Detail nájemní smlouvy (parametr: contractId). */
    public const val CONTRACT_DETAIL: String = "contracts/{contractId}"

    /** Obrazovka „Moje vybavení" (položky z aktivních smluv). */
    public const val MY_EQUIPMENT: String = "my-equipment"

    /** Obrazovka nahlášení poruchy (parametr: equipmentId). */
    public const val REPORT_ISSUE: String = "report-issue/{equipmentId}"

    /** Seznam servisních tiketů. */
    public const val TICKETS: String = "tickets"

    /** Detail servisního tiketu (parametr: ticketId). */
    public const val TICKET_DETAIL: String = "tickets/{ticketId}"

    /** Obrazovka vyřešení tiketu technikem (parametr: ticketId). */
    public const val TICKET_RESOLVE: String = "tickets/{ticketId}/resolve"

    /** Seznam plateb. */
    public const val PAYMENTS: String = "payments"

    /** Dashboard pro manažera/admina. */
    public const val DASHBOARD: String = "dashboard"

    /** Obrazovka AI asistenta (manažer/admin). */
    public const val ASSISTANT: String = "assistant"

    /** Správa katalogu (admin/manager) — CRUD nad vybavením. */
    public const val ADMIN_CATALOG: String = "admin-catalog"

    /** Prohlížeč PDF dokumentu (parametr: documentId). */
    public const val DOCUMENT: String = "document/{documentId}"

    /**
     * Vytvoří trasu detailu vybavení.
     * @param equipmentId ID vybavení
     * @return cesta trasy
     */
    public fun equipmentDetail(equipmentId: Long): String = "equipment/$equipmentId"

    /**
     * Vytvoří trasu pro vytvoření nové smlouvy.
     * @param ids seznam ID vybavení
     * @return cesta trasy
     */
    public fun contractNew(ids: List<Long>): String = "contract-new/${ids.joinToString(",")}"

    /**
     * Vytvoří trasu detailu smlouvy.
     * @param contractId ID smlouvy
     * @return cesta trasy
     */
    public fun contractDetail(contractId: Long): String = "contracts/$contractId"

    /**
     * Vytvoří trasu nahlášení poruchy.
     * @param equipmentId ID vybavení
     * @return cesta trasy
     */
    public fun reportIssue(equipmentId: Long): String = "report-issue/$equipmentId"

    /**
     * Vytvoří trasu detailu servisního tiketu.
     * @param ticketId ID tiketu
     * @return cesta trasy
     */
    public fun ticketDetail(ticketId: Long): String = "tickets/$ticketId"

    /**
     * Vytvoří trasu vyřešení tiketu technikem.
     * @param ticketId ID tiketu
     * @return cesta trasy
     */
    public fun ticketResolve(ticketId: Long): String = "tickets/$ticketId/resolve"

    /**
     * Vytvoří trasu prohlížeče PDF dokumentu.
     * @param documentId ID dokumentu
     * @return cesta trasy
     */
    public fun document(documentId: Long): String = "document/$documentId"
}
