package cz.b2brental.data.remote

import cz.b2brental.data.remote.dto.ErrorResponseDto
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

/**
 * Bezpečné volání API: zachytává ResponseException, IOException a SerializationException.
 * Při chybě parsingu — kód INTERNAL_ERROR, message z těla odpovědi (max. 250 znaků).
 * Při obdržení odpovědi 401 automaticky vymaže session přes [sessionClearer]
 * a publikuje událost [SessionEvents.unauthorized] pro přepnutí UI na přihlašovací obrazovku.
 * @param sessionClearer správce mazání session při vypršení tokenu
 * @param block asynchronní blok kódu obsahující HTTP požadavek
 * @return výsledek úspěšného volání
 * @throws ApiException při chybové odpovědi serveru
 * @throws OfflineException při nedostupnosti serveru
 */
public suspend fun <T> safeApiCall(sessionClearer: SessionClearer, block: suspend () -> T): T {
    return try {
        block()
    } catch (e: ResponseException) {
        val apiException = e.toApiException()
        if (apiException.httpStatus == 401) {
            clearSessionSafely(sessionClearer)
            SessionEvents.unauthorized.tryEmit(Unit)
        }
        throw apiException
    } catch (_: java.io.IOException) {
        throw OfflineException()
    } catch (_: SerializationException) {
        throw ApiException("INTERNAL_ERROR", 0, "Neplatná odpověď serveru")
    }
}

/**
 * Bezpečné vymazání session — nikdy nesmí vyhodit výjimku volajícímu kódu.
 * @param sessionClearer správce mazání session
 */
private suspend fun clearSessionSafely(sessionClearer: SessionClearer): Unit {
    try {
        sessionClearer.clearSession()
    } catch (_: Exception) {
        // Pohlcení výjimky — mazání session nesmí přerušit běh volajícího kódu
    }
}

/**
 * Převod Ktor ResponseException na ApiException s parsováním těla odpovědi.
 * @return ApiException s naparsovaným kódem, HTTP statusem a zprávou (max. 250 znaků)
 */
public suspend fun ResponseException.toApiException(): ApiException {
    val body = try {
        response.bodyAsText()
    } catch (_: Exception) {
        ""
    }
    return try {
        val parsed = B2bJson.decodeFromString<ErrorResponseDto>(body)
        ApiException(
            code = parsed.error.code,
            httpStatus = response.status.value,
            message = parsed.error.message.take(250),
        )
    } catch (_: Exception) {
        ApiException(
            code = "INTERNAL_ERROR",
            httpStatus = response.status.value,
            message = body.take(250).ifEmpty { "Vnitřní chyba serveru" },
        )
    }
}
