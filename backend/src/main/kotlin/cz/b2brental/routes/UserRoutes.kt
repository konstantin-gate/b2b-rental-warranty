@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental.routes

import cz.b2brental.db.Users
import cz.b2brental.models.TechnicianResponse
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.requireRole
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Seznam techniků pro přiřazení k tiketům; přístupné manažerům a administrátorům platformy i firmy.
 * PII (e-mail, telefon) je vždy maskováno.
 */
public fun Route.userRoutes() {
    route("/users") {
        authenticate("auth-jwt") {
            get {
                call.requireRole("manager", "admin")
                val role: String =
                    call.request.queryParameters["role"]
                        ?: throw BadRequestException("Parametr role je povinný")
                if (role != "technician") {
                    throw BadRequestException("Podporován je pouze filtr role=technician")
                }
                val technicians: List<TechnicianResponse> =
                    transaction {
                        Users
                            .selectAll()
                            .where { Users.role eq "technician" }
                            .orderBy(Users.email to SortOrder.ASC)
                            .map { row ->
                                val email: String = row[Users.email]
                                val maskedEmail: String =
                                    if (email.contains("@")) {
                                        val domain = email.substringAfter("@")
                                        email.take(2) + "***@$domain"
                                    } else {
                                        "***"
                                    }
                                val phone: String? = row[Users.phone]
                                val maskedPhone: String? = if (phone != null) "***" else null
                                TechnicianResponse(
                                    id = row[Users.id].value,
                                    email = maskedEmail,
                                    phone = maskedPhone,
                                )
                            }
                    }
                call.respond(technicians)
            }
        }
    }
}
