@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.models.AuthResponse
import cz.b2brental.models.LoginRequest
import cz.b2brental.models.RegisterCompanyRequest
import cz.b2brental.models.RegisterResponse
import cz.b2brental.services.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * Veřejné autentizační trasy (bez JWT ověření).
 * POST /auth/register-company → HTTP 201, POST /auth/login → HTTP 200.
 */
public fun Route.authRoutes(authService: AuthService) {
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
}
