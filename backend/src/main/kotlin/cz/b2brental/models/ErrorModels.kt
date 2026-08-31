package cz.b2brental.models

import kotlinx.serialization.Serializable

/** Jednotné tělo chybové odpovědi: {"error":{"code":"...","message":"..."}}. */
@Serializable
public data class ErrorBody(
    public val error: ErrorDetails,
)

/** Detail chyby: strojově čitelný kód a lidsky čitelná zpráva (max. 250 znaků). */
@Serializable
public data class ErrorDetails(
    public val code: String,
    public val message: String,
)
