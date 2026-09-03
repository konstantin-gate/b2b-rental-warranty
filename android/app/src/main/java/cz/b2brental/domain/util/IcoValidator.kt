package cz.b2brental.domain.util

/**
 * Validace IČO (identifikační číslo organizace) podle algoritmu Modulo 11.
 * Přesná kopie backend logiky z `backend/.../domain/Ico.kt:19-26`.
 */
public object IcoValidator {

    /**
     * Ověří, zda je řetězec platným IČO (8 číslic, kontrolní součet Modulo 11).
     * @param ico řetězec k ověření
     * @return true pokud je IČO platné
     */
    public fun isValid(ico: String): Boolean {
        if (ico.length != 8 || !ico.all { it.isDigit() }) return false
        val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2)
        val sum = weights.indices.sumOf { weights[it] * (ico[it] - '0') }
        val control = (11 - (sum % 11)) % 10
        return control == (ico[7] - '0')
    }
}
