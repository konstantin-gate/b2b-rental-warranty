package cz.b2brental.domain

import kotlinx.serialization.Serializable

/** E-mail s validací formátu při vytvoření; validace proběhne i při deserializaci DTO */
@Suppress("HardCodedStringLiteral")
@Serializable
@JvmInline
public value class Email(
    public val value: String,
) {
    init {
        require(value.length <= 200) { "E-mail je příliš dlouhý (max. 200 znaků)" }
        require(value.matches(EMAIL_REGEX)) { "Neplatný formát e-mailu: $value" }
    }

    public companion object {
        /** E-mail musí obsahovat @ a za @ alespoň jednu tečku, bez mezer */
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
