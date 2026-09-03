package cz.b2brental.data.remote

import cz.b2brental.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/**
 * Továrna na Ktor Client s konfigurací pro B2B Rental API.
 * Bez argumentu — production (CIO engine). S argumentem — pro testy (MockEngine).
 * @param engine volitelný engine (pro testy: MockEngine; pro produkci: CIO)
 */
public fun createB2bHttpClient(engine: HttpClientEngine? = null): HttpClient {
    return if (engine != null) {
        HttpClient(engine) {
            applyB2bConfig()
        }
    } else {
        HttpClient(CIO) {
            applyB2bConfig()
        }
    }
}

/**
 * Sdílená konfigurace B2B Rental pro HTTP klient.
 * Aplikuje: JSON serializaci, časové limity, logování a výchozí URL.
 */
private fun HttpClientConfig<*>.applyB2bConfig(): Unit {
    expectSuccess = true
    install(ContentNegotiation) { json(B2bJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
    install(Logging) {
        level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
    }
    defaultRequest {
        url(BuildConfig.API_BASE_URL)
        contentType(ContentType.Application.Json)
    }
}
