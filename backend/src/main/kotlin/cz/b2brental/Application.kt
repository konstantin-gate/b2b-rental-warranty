package cz.b2brental

import cz.b2brental.config.Config
import cz.b2brental.db.DatabaseFactory
import cz.b2brental.db.seed
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val config = Config.fromEnv()

    DatabaseFactory.connect(config.dbUrl, config.dbUser, config.dbPass)
    seed()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CallLogging)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}
