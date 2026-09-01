@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

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
                val ctx = call.callerContext()
                val companyId =
                    ctx.companyId
                        ?: throw ForbiddenException("Uživatel nemá přiřazenou společnost")
                val req = call.receive<TicketCreateRequest>()
                val response: TicketResponse = service.create(companyId, ctx.userId, req)
                call.respond(HttpStatusCode.Created, response)
            }

            get {
                call.requireRole("client", "technician", "manager", "admin")
                val ctx = call.callerContext()
                call.respond(service.list(ctx.userId, ctx.role, ctx.companyId))
            }

            get("/{id}") {
                call.requireRole("client", "technician", "manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val ctx = call.callerContext()
                call.respond(service.get(id, ctx.userId, ctx.role, ctx.companyId))
            }

            post("/{id}/assign") {
                call.requireRole("manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val ctx = call.callerContext()
                val req = call.receive<AssignRequest>()
                call.respond(service.assign(id, ctx.userId, req))
            }

            post("/{id}/start") {
                call.requireRole("technician")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val ctx = call.callerContext()
                call.respond(service.start(id, ctx.userId))
            }

            post("/{id}/resolve") {
                call.requireRole("technician")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::TicketId)
                        ?: throw BadRequestException("Neplatné id požadavku")
                val ctx = call.callerContext()
                val req = call.receive<ResolveRequest>()
                call.respond(service.resolve(id, ctx.userId, req))
            }

            post("/{id}/pdf") {
                call.requireRole("client", "technician", "manager", "admin")
                val id: Long =
                    call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Neplatné id tiketu")
                val ctx = call.callerContext()
                call.respond(HttpStatusCode.OK, service.pdfDocument(id, ctx.role, ctx.companyId, ctx.userId))
            }
        }
    }
}
