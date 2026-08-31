@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.domain.ContractId
import cz.b2brental.models.ContractCreateRequest
import cz.b2brental.services.ContractService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Registrace tras pro nájemní smlouvy */
public fun Route.contractRoutes(service: ContractService) {
    route("/contracts") {
        authenticate("auth-jwt") {
            post {
                call.requireRole("client")
                val principal = call.principal<JWTPrincipal>()
                val companyId =
                    principal?.get(JwtService.CLAIM_COMPANY_ID)?.toLongOrNull()
                        ?: throw ForbiddenException("Uživatel nemá přiřazenou společnost")
                val req = call.receive<ContractCreateRequest>()
                val response = service.create(companyId, req)
                call.respond(HttpStatusCode.Created, response)
            }

            get {
                call.requireRole("client", "manager", "admin")
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.get(JwtService.CLAIM_ROLE) ?: ""
                val companyId = principal?.get(JwtService.CLAIM_COMPANY_ID)?.toLongOrNull()
                call.respond(service.list(role, companyId))
            }

            get("/{id}") {
                call.requireRole("client", "manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::ContractId)
                        ?: throw BadRequestException("Neplatné id smlouvy")
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.get(JwtService.CLAIM_ROLE) ?: ""
                val companyId = principal?.get(JwtService.CLAIM_COMPANY_ID)?.toLongOrNull()
                call.respond(service.get(id, role, companyId))
            }

            post("/{id}/approve") {
                call.requireRole("manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::ContractId)
                        ?: throw BadRequestException("Neplatné id smlouvy")
                call.respond(service.approve(id))
            }

            post("/{id}/reject") {
                call.requireRole("manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::ContractId)
                        ?: throw BadRequestException("Neplatné id smlouvy")
                call.respond(service.reject(id))
            }
        }
    }
}
