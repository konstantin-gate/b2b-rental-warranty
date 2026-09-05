package cz.b2brental.presentation.util

/**
 * Jednotný typ chyby pro zobrazení v UI vrstvě.
 * Mapování na text probíhá v [cz.b2brental.presentation.components.errorMessage].
 */
public enum class ErrorType {
    /** Zařízení je offline — API není dosažitelné. */
    OFFLINE,
    /** Uživatel není přihlášen nebo vypršela platnost tokenu (HTTP 401). */
    UNAUTHORIZED,
    /** Uživatel nezadal datum zahájení pronájmu. */
    EMPTY_DATE,
    /** Uživatel nezadal adresu dodání. */
    EMPTY_ADDRESS,
    /** Nepodařilo se načíst sazby vybavení z katalogu. */
    RATES_LOAD_FAILED,
    /** Nepodařilo se vytvořit smlouvu na backendu. */
    CONTRACT_CREATE_FAILED,
    /** Nepodařilo se načíst seznam smluv. */
    CONTRACT_LIST_LOAD_FAILED,
    /** Nepodařilo se načíst detail smlouvy. */
    CONTRACT_DETAIL_LOAD_FAILED,
    /** Nepodařilo se schválit smlouvu. */
    CONTRACT_APPROVE_FAILED,
    /** Nepodařilo se zamítnout smlouvu. */
    CONTRACT_REJECT_FAILED,
    /** Nepodařilo se vygenerovat PDF smlouvy. */
    CONTRACT_PDF_FAILED,
    /** Nepodařilo se načíst katalog. */
    CATALOG_LOAD_FAILED,
    /** Nepodařilo se načíst detail vybavení. */
    EQUIPMENT_DETAIL_LOAD_FAILED,
    /** Nepodařilo se načíst «Mé vybavení». */
    MY_EQUIPMENT_LOAD_FAILED,
    /** V systému není nainstalována aplikace pro otevírání PDF. */
    PDF_NO_APP,
    /** Stažení PDF selhalo. */
    PDF_DOWNLOAD_FAILED,
    /** Uživatel nezadal popis poruchy. */
    EMPTY_DESCRIPTION,
    /** Popis poruchy je kratší než 10 znaků. */
    DESCRIPTION_TOO_SHORT,
    /** Zpracování (zmenšení/kódování) fotografie selhalo. */
    PHOTO_ENCODE_FAILED,
    /** Odeslání servisního hlášení na backend selhalo. */
    TICKET_CREATE_FAILED,
    /** Načtení seznamu hlášení selhalo. */
    TICKETS_LOAD_FAILED,
    /** Načtení detailu hlášení selhalo. */
    TICKET_DETAIL_LOAD_FAILED,
    /** Načtení seznamu plateb selhalo. */
    PAYMENTS_LOAD_FAILED,
    /** Zpracování platby selhalo. */
    PAYMENT_PAY_FAILED,
    /** Platba již byla dříve uhrazena (HTTP 409). */
    PAYMENT_ALREADY_PAID,
    /** Načtení metrik přehledu selhalo. */
    DASHBOARD_LOAD_FAILED,
    /** Načtení seznamu techniků selhalo. */
    TECHNICIANS_LOAD_FAILED,
    /** Přiřazení technika k tiketu selhalo. */
    TICKET_ASSIGN_FAILED,
    /** Zahájení opravy tiketu selhalo. */
    TICKET_START_FAILED,
    /** Vyřešení tiketu selhalo. */
    TICKET_RESOLVE_FAILED,
    /** Poznámky technika jsou prázdné. */
    NOTES_EMPTY,
    /** Kontaktování AI asistenta selhalo. */
    AI_ASSISTANT_FAILED,
    /** Validační chyba formuláře správy katalogu. */
    ADMIN_CATALOG_VALIDATION,
    /** Uložení záznamu katalogu selhalo. */
    ADMIN_CATALOG_SAVE_FAILED,
    /** Smazání záznamu katalogu selhalo. */
    ADMIN_CATALOG_DELETE_FAILED,
    /** Načtení katalogu pro správu selhalo. */
    ADMIN_CATALOG_LOAD_FAILED,
    /** Jakákoliv jiná (neočekávaná) chyba. */
    UNKNOWN,
}
