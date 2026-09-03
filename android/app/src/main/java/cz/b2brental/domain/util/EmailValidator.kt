package cz.b2brental.domain.util

/**
 * Klientská validace formátu e-mailové adresy.
 * Regex je přesná kopie backend logiky z `backend/.../domain/Email.kt:19`.
 * Backendová validace probíhá na straně serveru — tato kontrola slouží
 * pro okamžitou zpětnou vazbu na obrazovce registrace.
 */
public object EmailValidator {

    private val EMAIL_REGEX: Regex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /**
     * Ověří, zda řetězec odpovídá formátu e-mailové adresy.
     * @param email e-mailová adresa k ověření
     * @return true pokud formát odpovídá
     */
    public fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email)
}
