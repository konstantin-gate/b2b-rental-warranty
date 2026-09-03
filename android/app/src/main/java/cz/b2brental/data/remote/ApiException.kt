package cz.b2brental.data.remote

/**
 * Výjimka pro chyby API backendu.
 * @param code kód chyby z backendu (např. "UNAUTHORIZED", "CONFLICT")
 * @param httpStatus HTTP stavový kód
 * @param message text chyby (max. 250 znaků)
 */
public class ApiException(
    public val code: String,
    public val httpStatus: Int,
    message: String,
) : RuntimeException(message)

/**
 * Výjimka pro nedostupnost backendu (networking chyby).
 */
public class OfflineException : RuntimeException("Backend není dostupný")
