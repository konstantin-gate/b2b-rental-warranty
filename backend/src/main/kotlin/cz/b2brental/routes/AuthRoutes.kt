@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.auth.RevocationStore
import cz.b2brental.models.AuthResponse
import cz.b2brental.models.LoginRequest
import cz.b2brental.models.RegisterCompanyRequest
import cz.b2brental.models.RegisterResponse
import cz.b2brental.services.AuthService
import cz.b2brental.utils.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.time.Instant

/**
 * Veřejné trasy register-company a login (bez JWT ověření); POST /auth/logout vyžaduje platný token a zneplatní jej (200).
 * @param authService servis autentizace (registrace a přihlášení)
 * @param revocationStore úložiště odvolaných tokenů (zneplatnění při odhlášení)
 */
public fun Route.authRoutes(
    authService: AuthService,
    revocationStore: RevocationStore,
) {
    post("/auth/register-company") {
        val request: RegisterCompanyRequest = call.receive()
        val response: RegisterResponse = authService.registerCompany(request)
        call.respond(HttpStatusCode.Created, response)
    }

    post("/auth/login") {
        val request: LoginRequest = call.receive()
        val response: AuthResponse = authService.login(request)
        call.respond(response)
    }

    authenticate("auth-jwt") {
        post("/auth/logout") {
            val principal: JWTPrincipal =
                call.principal<JWTPrincipal>()
                    ?: throw UnauthorizedException("Autentizace je povinná pro tento požadavek")
            val jti: String =
                principal[JwtService.CLAIM_JWT_ID]
                    ?: throw UnauthorizedException("Chybí identifikátor tokenu")
            val expiresAt: Instant = principal.expiresAt?.toInstant() ?: Instant.now()
            revocationStore.revoke(jti, expiresAt)
            call.respond(mapOf("status" to "ok"))
        }
    }
}
