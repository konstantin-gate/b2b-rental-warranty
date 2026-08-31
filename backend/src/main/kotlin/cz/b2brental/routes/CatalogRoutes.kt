@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.domain.EquipmentCategoryId
import cz.b2brental.domain.EquipmentId
import cz.b2brental.models.CatalogUpsertRequest
import cz.b2brental.services.CatalogService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/** Registrace tras pro katalog vybavení */
public fun Route.catalogRoutes(service: CatalogService) {
    route("/catalog") {
        authenticate("auth-jwt") {
            // Čtení pro všechny přihlášené uživatele (libovolná role)
            get {
                val categoryIdRaw = call.request.queryParameters["categoryId"]
                val statusRaw = call.request.queryParameters["status"]
                val categoryId: EquipmentCategoryId? =
                    if (categoryIdRaw == null) {
                        null
                    } else {
                        categoryIdRaw.toLongOrNull()?.let(::EquipmentCategoryId)
                            ?: throw BadRequestException("Neplatné categoryId")
                    }
                call.respond(service.list(categoryId, statusRaw))
            }

            get("/{id}") {
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::EquipmentId)
                        ?: throw BadRequestException("Neplatné id vybavení")
                call.respond(service.get(id))
            }

            // Změny pouze pro role admin a manager
            post {
                call.requireRole("admin", "manager")
                val req = call.receive<CatalogUpsertRequest>()
                val id = service.create(req)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/{id}") {
                call.requireRole("admin", "manager")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::EquipmentId)
                        ?: throw BadRequestException("Neplatné id vybavení")
                val req = call.receive<CatalogUpsertRequest>()
                service.update(id, req)
                call.respond(service.get(id))
            }

            delete("/{id}") {
                call.requireRole("admin", "manager")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::EquipmentId)
                        ?: throw BadRequestException("Neplatné id vybavení")
                service.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
