@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.domain.ContractId
import cz.b2brental.models.ContractCreateRequest
import cz.b2brental.services.ContractService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.UnauthorizedException
import cz.b2brental.utils.jwtCompanyId
import cz.b2brental.utils.jwtRole
import cz.b2brental.utils.jwtScope
import cz.b2brental.utils.requirePlatform
import cz.b2brental.utils.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Získání typovaného ID smlouvy z parametrů požadavku */
private fun ApplicationCall.contractId(): ContractId =
    parameters["id"]?.toLongOrNull()?.let(::ContractId)
        ?: throw BadRequestException("Neplatné id smlouvy")

/**
 * Kontext pro generování PDF dokumentů.
 * @property id identifikátor smlouvy
 * @property role role volajícího uživatele
 * @property companyId id společnosti volajícího nebo null
 * @property userId id volajícího uživatele
 * @property scope scope volajícího (platform/tenant)
 */
private data class PdfContext(
    val id: ContractId,
    val role: String,
    val companyId: Long?,
    val userId: Long,
    val scope: String,
)

/** Extrakce kontextu z požadavku */
private fun ApplicationCall.pdfContext(): PdfContext {
    val id = contractId()
    val principal =
        principal<JWTPrincipal>()
            ?: throw UnauthorizedException("Chybí platný identifikátor uživatele v tokenu")
    return PdfContext(
        id = id,
        role = principal[JwtService.CLAIM_ROLE] ?: "",
        companyId = principal[JwtService.CLAIM_COMPANY_ID]?.toLongOrNull(),
        userId =
            principal.subject?.toLongOrNull()
                ?: throw UnauthorizedException("Chybí platný identifikátor uživatele v tokenu"),
        scope = principal[JwtService.CLAIM_SCOPE] ?: "",
    )
}

/**
 * Registrace tras pro nájemní smlouvy.
 * @param service servis pro správu nájemních smluv
 */
public fun Route.contractRoutes(service: ContractService) {
    route("/contracts") {
        authenticate("auth-jwt") {
            post {
                call.requireRole("client")
                val companyId = call.jwtCompanyId() ?: throw ForbiddenException("Uživatel nemá přiřazenou společnost")
                val req = call.receive<ContractCreateRequest>()
                val response = service.create(companyId, req)
                call.respond(HttpStatusCode.Created, response)
            }

            get {
                call.requireRole("client", "manager", "admin")
                call.respond(service.list(call.jwtRole(), call.jwtCompanyId(), call.jwtScope()))
            }

            get("/{id}") {
                call.requireRole("client", "manager", "admin")
                call.respond(service.get(call.contractId(), call.jwtRole(), call.jwtCompanyId(), call.jwtScope()))
            }

            post("/{id}/approve") {
                call.requirePlatform("manager", "admin")
                call.respond(service.approve(call.contractId()))
            }

            post("/{id}/reject") {
                call.requirePlatform("manager", "admin")
                call.respond(service.reject(call.contractId()))
            }

            post("/{id}/pdf") {
                call.requireRole("client", "manager", "admin")
                val ctx = call.pdfContext()
                call.respond(HttpStatusCode.OK, service.pdfDocument(ctx.id, ctx.role, ctx.companyId, ctx.userId, ctx.scope))
            }

            post("/{id}/acceptance-act") {
                call.requireRole("client", "manager", "admin")
                val ctx = call.pdfContext()
                call.respond(HttpStatusCode.OK, service.acceptanceActDocument(ctx.id, ctx.role, ctx.companyId, ctx.userId, ctx.scope))
            }

            post("/{id}/return-act") {
                call.requireRole("client", "manager", "admin")
                val ctx = call.pdfContext()
                call.respond(HttpStatusCode.OK, service.returnActDocument(ctx.id, ctx.role, ctx.companyId, ctx.userId, ctx.scope))
            }
        }
    }
}
