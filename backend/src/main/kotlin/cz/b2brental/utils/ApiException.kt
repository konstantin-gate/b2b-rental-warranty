@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.utils

import io.ktor.http.HttpStatusCode

/**
 * Základní zapečetěná výjimka API. Nese HTTP status a kód chyby
 * pro jednotný formát odpovědí {"error":{"code":"...","message":"..."}}.
 * Podtřídy jsou deklarované na nejvyšší úrovni v tomto souboru.
 */
public sealed class ApiException(
    public val code: String,
    public val httpStatus: HttpStatusCode,
    message: String,
) : RuntimeException(message)

/** Chyba vstupních dat: HTTP 400, kód VALIDATION_ERROR. */
public class BadRequestException(
    message: String,
    code: String = "VALIDATION_ERROR",
) : ApiException(code, HttpStatusCode.BadRequest, message)

/** Neautentizovaný přístup: HTTP 401, kód UNAUTHORIZED. */
public class UnauthorizedException(
    message: String,
    code: String = "UNAUTHORIZED",
) : ApiException(code, HttpStatusCode.Unauthorized, message)

/** Přístup odepřen rolí: HTTP 403, kód FORBIDDEN. */
public class ForbiddenException(
    message: String,
    code: String = "FORBIDDEN",
) : ApiException(code, HttpStatusCode.Forbidden, message)

/** Entita nenalezena: HTTP 404, kód NOT_FOUND. */
public class NotFoundException(
    message: String,
    code: String = "NOT_FOUND",
) : ApiException(code, HttpStatusCode.NotFound, message)

/** Konflikt stavu: HTTP 409, kód CONFLICT. */
public class ConflictException(
    message: String,
    code: String = "CONFLICT",
) : ApiException(code, HttpStatusCode.Conflict, message)
