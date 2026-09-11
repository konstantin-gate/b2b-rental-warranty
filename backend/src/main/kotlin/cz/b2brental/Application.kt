@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental

import com.auth0.jwt.algorithms.Algorithm
import cz.b2brental.auth.JwtService
import cz.b2brental.auth.LoginRateLimiter
import cz.b2brental.auth.RevocationStore
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
import cz.b2brental.routes.notificationRoutes
import cz.b2brental.routes.paymentRoutes
import cz.b2brental.routes.ticketRoutes
import cz.b2brental.routes.userRoutes
import cz.b2brental.services.AiService
import cz.b2brental.services.AuthService
import cz.b2brental.services.CatalogService
import cz.b2brental.services.ContractService
import cz.b2brental.services.DashboardService
import cz.b2brental.services.KnowledgeBaseService
import cz.b2brental.services.KnowledgeIndexService
import cz.b2brental.services.NotificationService
import cz.b2brental.services.PaymentService
import cz.b2brental.services.PdfService
import cz.b2brental.services.TicketService
import cz.b2brental.services.llm.DisabledLlmClient
import cz.b2brental.services.llm.LlmClient
import cz.b2brental.services.llm.OpenAiCompatibleLlmClient
import cz.b2brental.utils.ApiException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
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
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.slf4j.LoggerFactory
import java.time.Clock

/** Logger neošetřených chyb; detail jde jen do logu, nikdy do těla odpovědi. */
private val errorLogger = LoggerFactory.getLogger("cz.b2brental.status")

/** SQLState porušení unikátního indexu (PostgreSQL i H2) */
private const val UNIQUE_VIOLATION_SQL_STATE: String = "23505"

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
    if (config.seedDemoData) seed()
    val knowledgeIndexService = KnowledgeIndexService()
    knowledgeIndexService.synchronize()

    val jwtService = JwtService(config.jwtSecret)
    val loginRateLimiter = LoginRateLimiter()
    val revocationStore = RevocationStore()
    val authService = AuthService(jwtService, loginRateLimiter)
    val llmClient: LlmClient =
        if (config.aiEnabled) {
            OpenAiCompatibleLlmClient(config)
        } else {
            DisabledLlmClient
        }
    val aiService = AiService(llmClient)
    val knowledgeBaseService = KnowledgeBaseService()

    monitor.subscribe(ApplicationStopping) {
        aiService.close()
    }

    val catalogService = CatalogService()
    val notificationService = NotificationService()
    val contractService = ContractService(notificationService)
    val paymentService = PaymentService(Clock.systemDefaultZone())
    val ticketService = TicketService(aiService, knowledgeBaseService, notificationService)
    val pdfService = PdfService()
    val dashboardService = DashboardService(paymentService, Clock.systemDefaultZone())

    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header(HttpHeaders.CacheControl, "no-store")
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = false
                ignoreUnknownKeys = true
            },
        )
    }

    // Omezení velikosti těla požadavku: odmítá se příliš velký Content-Length i přenos chunked,
    // u kterého Content-Length chybí (limit 8 388 608 bajtů).
    intercept(ApplicationCallPipeline.Plugins) {
        val contentLength: Long = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
        val chunked: Boolean =
            call.request.headers[HttpHeaders.TransferEncoding]
                ?.split(',')
                ?.any { value -> value.trim().equals("chunked", ignoreCase = true) } == true
        if (chunked || contentLength > 8_388_608L) {
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ErrorBody(
                    ErrorDetails(
                        code = "PAYLOAD_TOO_LARGE",
                        message = "Tělo požadavku je příliš velké (max. 8 MB)",
                    ),
                ),
            )
            finish()
        }
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

        // 2) Chyby deserializace těla požadavku (Ktor 3.x) — odpověď nikdy neobsahuje fragment vstupu.
        //    Zachovává se pouze smysluplná zpráva vlastních value tříd Email/Ico.
        exception<BadRequestException> { call, cause ->
            val nested: Throwable? = cause.cause?.cause ?: cause.cause
            val message: String =
                if (nested is IllegalArgumentException && nested !is SerializationException) {
                    nested.message ?: "Neplatná vstupní data"
                } else {
                    "Neplatná vstupní data"
                }
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(ErrorDetails(code = "VALIDATION_ERROR", message = message.take(250))),
            )
        }

        // 3) Tělo požadavku nelze transformovat (prázdné tělo, nepodporovaný Content-Type) → 400.
        exception<ContentTransformationException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorBody(
                    ErrorDetails(
                        code = "VALIDATION_ERROR",
                        message = "Neplatná vstupní data",
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

        // 4a) Porušení unikátního indexu při souběhu zápisů → 409 místo 500.
        exception<ExposedSQLException> { call, cause ->
            if (cause.sqlState == UNIQUE_VIOLATION_SQL_STATE) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorBody(
                        ErrorDetails(
                            code = "CONFLICT",
                            message = "Záznam již existuje",
                        ),
                    ),
                )
            } else {
                errorLogger.error("Neošetřená chyba na ${call.request.uri}", cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorBody(ErrorDetails(code = "INTERNAL_ERROR", message = "Vnitřní chyba serveru")),
                )
            }
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
                // Odmítnutí zneplatněných tokenů (jti revocation)
                val jti: String? = credential.payload.getClaim(JwtService.CLAIM_JWT_ID)?.asString()
                if (jti != null && revocationStore.isRevoked(jti)) return@validate null
                // Token bez platného scope (staré tokeny před deployem) je odmítnut → vynutí opětovné přihlášení
                val scope: String? = credential.payload.getClaim(JwtService.CLAIM_SCOPE)?.asString()
                if (scope != JwtService.SCOPE_PLATFORM && scope != JwtService.SCOPE_TENANT) return@validate null
                // Subject musí být číslo; jinak token odmítnout (ochrana proti userId → 0L)
                val subject: String? = credential.payload.subject
                if (subject?.toLongOrNull() == null) return@validate null
                // Token bez expirace je odmítnut; bez exp by nebylo možné jej spolehlivě odvolat
                if (credential.expiresAt == null) return@validate null
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
        authRoutes(authService, revocationStore)
        catalogRoutes(catalogService)
        contractRoutes(contractService)
        paymentRoutes(paymentService)
        ticketRoutes(ticketService)
        documentRoutes(pdfService)
        aiRoutes(aiService, dashboardService, knowledgeBaseService)
        dashboardRoutes(dashboardService)
        userRoutes()
        notificationRoutes(notificationService)
    }
}
