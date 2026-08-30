package cz.b2brental

import cz.b2brental.config.Config
import cz.b2brental.db.DatabaseFactory
import cz.b2brental.db.seed
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

/** Vstupní bod backendové aplikace */
public fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

/** Konfigurace a inicializace Ktor modulu */
public fun Application.module() {
    val config = Config.fromEnv()

    DatabaseFactory.connect(config.dbUrl, config.dbUser, config.dbPass)
    seed()

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    install(CallLogging)

    @Suppress("HardCodedStringLiteral")
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}
