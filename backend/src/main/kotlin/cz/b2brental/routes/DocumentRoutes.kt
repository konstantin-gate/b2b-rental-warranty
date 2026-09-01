@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.db.ContractItems
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.domain.DocumentId
import cz.b2brental.services.PdfService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.NotFoundException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Registrace tras pro stahování PDF dokumentů */
public fun Route.documentRoutes(pdfService: PdfService) {
    route("/documents") {
        authenticate("auth-jwt") {
            get("/{id}/pdf") {
                val principal =
                    call.principal<JWTPrincipal>()
                        ?: throw ForbiddenException("Neplatný uživatel")
                val role = principal[JwtService.CLAIM_ROLE] ?: ""
                val companyId = principal[JwtService.CLAIM_COMPANY_ID]?.toLongOrNull()
                val userId = principal.subject?.toLongOrNull() ?: 0L

                val id =
                    call.parameters["id"]?.toLongOrNull()?.let(::DocumentId)
                        ?: throw BadRequestException("Neplatné id dokumentu")

                val docRow =
                    transaction {
                        Documents.selectAll().where { Documents.id eq id.value }.singleOrNull()
                    } ?: throw NotFoundException("Dokument nenalezen")

                val docType = docRow[Documents.type]
                val entityType = docRow[Documents.entityType]
                val entityId = docRow[Documents.entityId]

                val isAllowed = checkDocumentAccess(docType, entityType, entityId, role, companyId, userId)
                if (!isAllowed) {
                    throw ForbiddenException("Nemáte přístup k tomuto dokumentu")
                }

                val bytes = pdfService.render(docType, entityId)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    "inline; filename=\"${docType.name}-${id.value}.pdf\"",
                )
                call.respondBytes(bytes, ContentType.Application.Pdf, HttpStatusCode.OK)
            }
        }
    }
}

private fun checkDocumentAccess(
    docType: DocumentType,
    entityType: String,
    entityId: Long,
    role: String,
    companyId: Long?,
    userId: Long,
): Boolean {
    if (role == "admin" || role == "manager") return true

    if (role == "technician") {
        return docType == DocumentType.service_report &&
            entityType == "ticket" &&
            transaction {
                val ticket = ServiceTickets.selectAll().where { ServiceTickets.id eq entityId }.singleOrNull()
                ticket?.get(ServiceTickets.technicianId)?.value == userId
            }
    }

    if (role == "client") {
        return companyId != null &&
            transaction {
                when (entityType) {
                    "contract" -> {
                        val contract = RentalContracts.selectAll().where { RentalContracts.id eq entityId }.singleOrNull()
                        contract?.get(RentalContracts.companyId)?.value == companyId
                    }
                    "ticket" -> {
                        val ticket = ServiceTickets.selectAll().where { ServiceTickets.id eq entityId }.singleOrNull()
                        ticket?.get(ServiceTickets.companyId)?.value == companyId
                    }
                    "payment" -> {
                        val payment = Payments.selectAll().where { Payments.id eq entityId }.singleOrNull()
                        if (payment == null) {
                            false
                        } else {
                            val contract =
                                RentalContracts
                                    .selectAll()
                                    .where { RentalContracts.id eq payment[Payments.contractId].value }
                                    .singleOrNull()
                            contract?.get(RentalContracts.companyId)?.value == companyId
                        }
                    }
                    "equipment" -> {
                        val contracts =
                            (ContractItems innerJoin RentalContracts)
                                .selectAll()
                                .where { ContractItems.equipmentId eq entityId }
                                .map { it[RentalContracts.companyId].value }
                        companyId in contracts
                    }
                    else -> false
                }
            }
    }

    return false
}
