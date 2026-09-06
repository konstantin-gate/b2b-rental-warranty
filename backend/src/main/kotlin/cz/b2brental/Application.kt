@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental

import com.auth0.jwt.algorithms.Algorithm
import cz.b2brental.auth.JwtService
import cz.b2brental.config.Config
import cz.b2brental.db.DatabaseFactory
import cz.b2brental.db.seed
import cz.b2brental.models.ErrorBody
import cz.b2brental.models.ErrorDetails
import cz.b2brental.routes.aiRoutes
import cz.b2brental.routes.authRoutes
import cz.b2brental.routes.catalogRoutes
import cz.b2brental.routes.contractRoutes
import cz.b2brental.routes.dashboardRoutes
import cz.b2brental.routes.documentRoutes
import cz.b2brental.routes.paymentRoutes
import cz.b2brental.routes.ticketRoutes
import cz.b2brental.routes.userRoutes
import cz.b2brental.services.AiService
import cz.b2brental.services.AuthService
import cz.b2brental.services.CatalogService
import cz.b2brental.services.ContractService
import cz.b2brental.services.DashboardService
import cz.b2brental.services.KnowledgeIndexService
import cz.b2brental.services.PaymentService
import cz.b2brental.services.PdfService
import cz.b2brental.services.TicketService
import cz.b2brental.services.llm.DisabledLlmClient
import cz.b2brental.services.llm.LlmClient
import cz.b2brental.services.llm.OpenAiCompatibleLlmClient
import cz.b2brental.utils.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Clock

/** Logger neošetřených chyb; detail jde jen do logu, nikdy do těla odpovědi. */
private val errorLogger = LoggerFactory.getLogger("cz.b2brental.status")

/** Vstupní bod backendové aplikace */
public fun main() {
    embeddedServer(Netty, port = 8090) {
        module()
    }.start(wait = true)
}

/**
 * Konfigurace a inicializace Ktor modulu.
 * @param config konfigurace aplikace načtená z proměnných prostředí
 */
public fun Application.module(config: Config = Config.fromEnv()) {
    DatabaseFactory.connect(config.dbUrl, config.dbUser, config.dbPass)
    seed()
    val knowledgeIndexService = KnowledgeIndexService()
    knowledgeIndexService.synchronize()

    val jwtService = JwtService(config.jwtSecret)
    val authService = AuthService(jwtService)
    val llmClient: LlmClient =
        if (config.aiEnabled) {
            OpenAiCompatibleLlmClient(config)
        } else {
            DisabledLlmClient
        }
    val aiService = AiService(llmClient)

    monitor.subscribe(ApplicationStopping) {
        aiService.close()
    }

    val catalogService = CatalogService()
    val contractService = ContractService()
    val paymentService = PaymentService(Clock.systemDefaultZone())
    val ticketService = TicketService(aiService)
    val pdfService = PdfService()
    val dashboardService = DashboardService(paymentService, Clock.systemDefaultZone())

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    install(StatusPages) {
        // 1) Vlastní výjimky API — status a kód se berou z výjimky.
        exception<ApiException> { call, cause ->
            call.respond(
                cause.httpStatus,
                ErrorBody(
                    ErrorDetails(
                        code = cause.code,
                        message = (cause.message ?: "Neplatná vstupní data").take(250),
                    ),
                ),
            )
        }

        // 2) Chyby deserializace těla požadavku — Ktor 3.x je zabaluje dvakrát:
        //    RequestConverter → BadRequestException("Failed to convert request body to …"),
        //    konvertér kotlinx-json → JsonConvertException("Illegal input: …");
        //    smysluplná česká zpráva (init{} value tříd Email/Ico) je proto v cause.cause.cause.
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(
                    ErrorDetails(
                        code = "VALIDATION_ERROR",
                        message =
                            (
                                cause.cause?.cause?.message
                                    ?: cause.cause?.message
                                    ?: cause.message
                                    ?: "Neplatná vstupní data"
                            ).take(250),
                    ),
                ),
            )
        }

        // 3) Tělo požadavku nelze transformovat (prázdné tělo, nepodporovaný Content-Type) → 400.
        exception<ContentTransformationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(
                    ErrorDetails(
                        code = "VALIDATION_ERROR",
                        message = (cause.message ?: "Neplatná vstupní data").take(250),
                    ),
                ),
            )
        }

        // 3a) Ktor 3.x: nelze transformovat tělo požadavku (prázdné tělo, chybějící/nepodporovaný
        //     Content-Type) — engine sám odpoví 415 bez těla, mimo StatusPages exception;
        //     zachycujeme status hookem a vracíme jednotný formát 400 VALIDATION_ERROR.
        status(HttpStatusCode.UnsupportedMediaType) { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(
                    ErrorDetails(
                        code = "VALIDATION_ERROR",
                        message = "Nepodporovaný nebo chybějící Content-Type, nebo prázdné tělo požadavku",
                    ),
                ),
            )
        }

        // 4) Porušení require{} v servisní vrstvě → 400.
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(
                    ErrorDetails(
                        code = "VALIDATION_ERROR",
                        message = (cause.message ?: "Neplatná vstupní data").take(250),
                    ),
                ),
            )
        }

        // 5) Vše ostatní → 500; detail výjimky jen do logu, tělo odpovědi je konstantní.
        exception<Throwable> { call, cause ->
            errorLogger.error("Neošetřená chyba na ${call.request.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorBody(ErrorDetails(code = "INTERNAL_ERROR", message = "Vnitřní chyba serveru")),
            )
        }
    }

    install(Authentication) {
        jwt(name = "auth-jwt") {
            verifier(
                issuer = JwtService.ISSUER,
                audience = JwtService.AUDIENCE,
                algorithm = Algorithm.HMAC256(config.jwtSecret),
            )
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorBody(
                        ErrorDetails(
                            code = "UNAUTHORIZED",
                            message = "Ověření selhalo (chybný nebo chybějící token)",
                        ),
                    ),
                )
            }
            realm = "b2b-rental"
        }
    }

    install(CallLogging)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        authRoutes(authService)
        catalogRoutes(catalogService)
        contractRoutes(contractService)
        paymentRoutes(paymentService)
        ticketRoutes(ticketService)
        documentRoutes(pdfService)
        aiRoutes(aiService, dashboardService)
        dashboardRoutes(dashboardService)
        userRoutes()
    }
}
