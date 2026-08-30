package cz.b2brental.domain

import kotlinx.serialization.Serializable

/** IČO — identifikační číslo osoby (ČR): přesně 8 číslic + kontrolní součet modulo 11 */
@Suppress("HardCodedStringLiteral")
@Serializable
@JvmInline
public value class Ico(
    public val value: String,
) {
    init {
        require(value.matches(Regex("^\\d{8}$"))) { "IČO musí obsahovat přesně 8 číslic: $value" }
        require(isChecksumValid(value)) { "Kontrolní součet IČO neodpovídá: $value" }
    }

    public companion object {
        /** Váhy 8,7,6,5,4,3,2; kontrolní číslice x = (11 - (součet mod 11)) mod 10 */
        public fun isChecksumValid(ico: String): Boolean {
            val digits = ico.map { it - '0' }
            val sum =
                digits[0] * 8 + digits[1] * 7 + digits[2] * 6 +
                    digits[3] * 5 + digits[4] * 4 + digits[5] * 3 + digits[6] * 2
            val control = (11 - (sum % 11)) % 10
            return digits[7] == control
        }
    }
}
