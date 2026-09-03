package cz.b2brental.domain.util

/**
 * Klientská validace formátu telefonního čísla.
 * Backend telefon nevaliduje a přijímá null — tato kontrola se používá
 * pouze pro vyplněné pole na obrazovce registrace.
 */
public object PhoneValidator {

    private val PHONE_REGEX: Regex = Regex("^\\+420\\d{9}$")

    /**
     * Ověří, zda řetězec odpovídá formátu +420 + 9 číslic.
     * @param phone telefonní číslo k ověření
     * @return true pokud formát odpovídá
     */
    public fun isValid(phone: String): Boolean = PHONE_REGEX.matches(phone)
}
