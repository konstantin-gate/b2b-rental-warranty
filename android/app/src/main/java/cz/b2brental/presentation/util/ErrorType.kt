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
    /** Jakákoliv jiná (neočekávaná) chyba. */
    UNKNOWN,
}
