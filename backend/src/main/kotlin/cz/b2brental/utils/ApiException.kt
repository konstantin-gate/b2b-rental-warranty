@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.utils

import io.ktor.http.HttpStatusCode

/**
 * Základní zapečetěná výjimka API. Nese HTTP status a kód chyby
 * pro jednotný formát odpovědí {"error":{"code":"...","message":"..."}}.
 * Podtřídy jsou deklarované na nejvyšší úrovni v tomto souboru.
 * @property code kód chyby pro odpověď API
 * @property httpStatus HTTP status odpovědi
 * @param message lidsky čitelná zpráva chyby
 */
public sealed class ApiException(
    public val code: String,
    public val httpStatus: HttpStatusCode,
    message: String,
) : RuntimeException(message)

/**
 * Chyba vstupních dat: HTTP 400, kód VALIDATION_ERROR.
 * @param message lidsky čitelná zpráva chyby
 * @param code kód chyby pro odpověď API
 */
public class BadRequestException(
    message: String,
    code: String = "VALIDATION_ERROR",
) : ApiException(code, HttpStatusCode.BadRequest, message)

/**
 * Neautentizovaný přístup: HTTP 401, kód UNAUTHORIZED.
 * @param message lidsky čitelná zpráva chyby
 * @param code kód chyby pro odpověď API
 */
public class UnauthorizedException(
    message: String,
    code: String = "UNAUTHORIZED",
) : ApiException(code, HttpStatusCode.Unauthorized, message)

/**
 * Přístup odepřen rolí: HTTP 403, kód FORBIDDEN.
 * @param message lidsky čitelná zpráva chyby
 * @param code kód chyby pro odpověď API
 */
public class ForbiddenException(
    message: String,
    code: String = "FORBIDDEN",
) : ApiException(code, HttpStatusCode.Forbidden, message)

/**
 * Entita nenalezena: HTTP 404, kód NOT_FOUND.
 * @param message lidsky čitelná zpráva chyby
 * @param code kód chyby pro odpověď API
 */
public class NotFoundException(
    message: String,
    code: String = "NOT_FOUND",
) : ApiException(code, HttpStatusCode.NotFound, message)

/**
 * Konflikt stavu: HTTP 409, kód CONFLICT.
 * @param message lidsky čitelná zpráva chyby
 * @param code kód chyby pro odpověď API
 */
public class ConflictException(
    message: String,
    code: String = "CONFLICT",
) : ApiException(code, HttpStatusCode.Conflict, message)

/**
 * Příliš mnoho požadavků (rate limit): HTTP 429, kód TOO_MANY_REQUESTS.
 * @param message lidsky čitelná zpráva chyby
 * @param code kód chyby pro odpověď API
 */
public class TooManyRequestsException(
    message: String,
    code: String = "TOO_MANY_REQUESTS",
) : ApiException(code, HttpStatusCode.TooManyRequests, message)
