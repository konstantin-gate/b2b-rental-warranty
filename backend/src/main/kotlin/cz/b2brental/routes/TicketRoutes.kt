@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.domain.TicketId
import cz.b2brental.models.AssignRequest
import cz.b2brental.models.ResolveRequest
import cz.b2brental.models.TicketCreateRequest
import cz.b2brental.models.TicketResponse
import cz.b2brental.services.TicketService
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

/** Registrace tras pro servisní požadavky */
public fun Route.ticketRoutes(service: TicketService) {
    route("/tickets") {
        authenticate("auth-jwt") {
            post {
                call.requireRole("client")
                val principal: JWTPrincipal =
                    call.principal<JWTPrincipal>() ?: throw ForbiddenException("Neplatný uživatel")
                val userId: Long =
                    principal.subject?.toLongOrNull()
                        ?: throw ForbiddenException("Neplatný uživatel")
                val companyId: Long =
                    principal[JwtService.CLAIM_COMPANY_ID]?.toLongOrNull()
                        ?: throw ForbiddenException("Uživatel nemá přiřazenou společnost")
                val req = call.receive<TicketCreateRequest>()
                val response: TicketResponse = service.create(companyId, userId, req)
                call.respond(HttpStatusCode.Created, response)
            }

            get {
                call.requireRole("client", "technician", "manager", "admin")
                val principal: JWTPrincipal =
                    call.principal<JWTPrincipal>() ?: throw ForbiddenException("Neplatný uživatel")
                val userId: Long =
                    principal.subject?.toLongOrNull()
                        ?: throw ForbiddenException("Neplatný uživatel")
                val role: String = principal[JwtService.CLAIM_ROLE] ?: ""
                val companyId: Long? = principal[JwtService.CLAIM_COMPANY_ID]?.toLongOrNull()
                call.respond(service.list(userId, role, companyId))
            }

            get("/{id}") {
                call.requireRole("client", "technician", "manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val principal: JWTPrincipal =
                    call.principal<JWTPrincipal>() ?: throw ForbiddenException("Neplatný uživatel")
                val userId: Long =
                    principal.subject?.toLongOrNull()
                        ?: throw ForbiddenException("Neplatný uživatel")
                val role: String = principal[JwtService.CLAIM_ROLE] ?: ""
                val companyId: Long? = principal[JwtService.CLAIM_COMPANY_ID]?.toLongOrNull()
                call.respond(service.get(id, userId, role, companyId))
            }

            post("/{id}/assign") {
                call.requireRole("manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val principal: JWTPrincipal =
                    call.principal<JWTPrincipal>() ?: throw ForbiddenException("Neplatný uživatel")
                val userId: Long =
                    principal.subject?.toLongOrNull()
                        ?: throw ForbiddenException("Neplatný uživatel")
                val req = call.receive<AssignRequest>()
                call.respond(service.assign(id, userId, req))
            }

            post("/{id}/start") {
                call.requireRole("technician")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val principal: JWTPrincipal =
                    call.principal<JWTPrincipal>() ?: throw ForbiddenException("Neplatný uživatel")
                val userId: Long =
                    principal.subject?.toLongOrNull()
                        ?: throw ForbiddenException("Neplatný uživatel")
                call.respond(service.start(id, userId))
            }

            post("/{id}/resolve") {
                call.requireRole("technician")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val principal: JWTPrincipal =
                    call.principal<JWTPrincipal>() ?: throw ForbiddenException("Neplatný uživatel")
                val userId: Long =
                    principal.subject?.toLongOrNull()
                        ?: throw ForbiddenException("Neplatný uživatel")
                val req = call.receive<ResolveRequest>()
                call.respond(service.resolve(id, userId, req))
            }
        }
    }
}
