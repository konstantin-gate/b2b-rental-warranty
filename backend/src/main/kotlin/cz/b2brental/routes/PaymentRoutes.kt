@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.domain.ContractId
import cz.b2brental.domain.PaymentId
import cz.b2brental.services.PaymentService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.requireRole
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Registrace tras pro platby */
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

                val principal = call.principal<JWTPrincipal>()
                val role: String = principal?.get(JwtService.CLAIM_ROLE) ?: ""
                val companyId: Long? = principal?.get(JwtService.CLAIM_COMPANY_ID)?.toLongOrNull()

                call.respond(service.list(role, companyId, contractId))
            }

            post("/{id}/pay") {
                call.requireRole("client", "manager", "admin")
                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::PaymentId)
                        ?: throw BadRequestException("Neplatné id platby")

                val principal = call.principal<JWTPrincipal>()
                val role: String = principal?.get(JwtService.CLAIM_ROLE) ?: ""
                val companyId: Long? = principal?.get(JwtService.CLAIM_COMPANY_ID)?.toLongOrNull()

                call.respond(service.pay(id, role, companyId))
            }
        }
    }
}
