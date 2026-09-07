@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.models.UnreadCountResponse
import cz.b2brental.services.NotificationService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.requireRole
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Registrace tras pro notifikace uživatele (všechny role)
 * @param service služba notifikací
 */
public fun Route.notificationRoutes(service: NotificationService) {
    route("/notifications") {
        authenticate("auth-jwt") {
            get("/unread-count") {
                call.requireRole("client", "technician", "manager", "admin")
                val ctx = call.callerContext()
                call.respond(UnreadCountResponse(service.unreadCount(ctx.userId)))
            }

            post("/read-all") {
                call.requireRole("client", "technician", "manager", "admin")
                val ctx = call.callerContext()
                call.respond(service.markAllRead(ctx.userId))
            }

            get {
                call.requireRole("client", "technician", "manager", "admin")
                val unreadParam: String? = call.request.queryParameters["unread"]
                val onlyUnread: Boolean =
                    when (unreadParam) {
                        null -> false
                        "true" -> true
                        "false" -> false
                        else -> throw BadRequestException("Neplatná hodnota parametru unread")
                    }
                val ctx = call.callerContext()
                call.respond(service.list(ctx.userId, onlyUnread))
            }

            post("/{id}/read") {
                call.requireRole("client", "technician", "manager", "admin")
                val id: Long =
                    call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Neplatné id požadavku")
                val ctx = call.callerContext()
                call.respond(service.markRead(ctx.userId, id))
            }
        }
    }
}
