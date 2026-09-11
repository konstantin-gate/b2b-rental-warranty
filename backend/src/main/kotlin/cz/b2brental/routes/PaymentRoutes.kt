@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.domain.ContractId
import cz.b2brental.domain.PaymentId
import cz.b2brental.services.PaymentService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Registrace tras pro platby.
 * @param service služba pro operace s platebními údaji (list, pay, pdfDocument).
 */
public fun Route.paymentRoutes(service: PaymentService) {
    route("/payments") {
        authenticate("auth-jwt") {
            get {
                call.requireRole("client", "manager", "admin")
                val contractIdRaw = call.request.queryParameters["contract_id"]
                val contractId: ContractId? =
                    if (contractIdRaw == null) {
                        null
                    } else {
                        contractIdRaw.toLongOrNull()?.let(::ContractId)
                            ?: throw BadRequestException("Neplatné contract_id")
                    }

                val ctx = call.callerContext()
                call.respond(service.list(ctx.role, ctx.companyId, contractId, ctx.scope))
            }

            post("/{id}/pay") {
                call.requireRole("client", "manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::PaymentId)
                        ?: throw BadRequestException("Neplatné id platby")

                val ctx = call.callerContext()
                call.respond(service.pay(id, ctx.role, ctx.companyId, ctx.scope))
            }

            post("/{id}/pdf") {
                call.requireRole("client", "manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::PaymentId)
                        ?: throw BadRequestException("Neplatné id platby")

                val ctx = call.callerContext()
                call.respond(HttpStatusCode.OK, service.pdfDocument(id, ctx.role, ctx.companyId, ctx.userId, ctx.scope))
            }
        }
    }
}
