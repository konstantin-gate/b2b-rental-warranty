@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.domain.DocumentId
import cz.b2brental.models.DocumentResponse
import cz.b2brental.services.PdfService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.NotFoundException
import cz.b2brental.utils.UnauthorizedException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Kontext přihlášeného uživatele pro kontrolu přístupu.
 * @property role role volajícího
 * @property companyId id společnosti volajícího nebo null
 * @property userId id volajícího uživatele
 * @property scope scope volajícího (platform/tenant)
 */
internal data class CallerContext(
    val role: String,
    val companyId: Long?,
    val userId: Long,
    val scope: String,
)

internal fun io.ktor.server.application.ApplicationCall.callerContext(): CallerContext {
    val principal =
        principal<JWTPrincipal>()
            ?: throw ForbiddenException("Neplatný uživatel")
    return CallerContext(
        role = principal[JwtService.CLAIM_ROLE] ?: "",
        companyId = principal[JwtService.CLAIM_COMPANY_ID]?.toLongOrNull(),
        userId =
            principal.subject?.toLongOrNull()
                ?: throw UnauthorizedException("Chybí platný identifikátor uživatele v tokenu"),
        scope = principal[JwtService.CLAIM_SCOPE] ?: "",
    )
}

/**
 * Registrace tras pro přehled a stahování PDF dokumentů.
 * @param pdfService servis pro generování binárního obsahu PDF dokumentů
 */
public fun Route.documentRoutes(pdfService: PdfService) {
    route("/documents") {
        authenticate("auth-jwt") {
            get {
                val ctx = call.callerContext()

                val contractIdParam = call.request.queryParameters["contract_id"]?.toLongOrNull()
                val ticketIdParam = call.request.queryParameters["ticket_id"]?.toLongOrNull()
                val paymentIdParam = call.request.queryParameters["payment_id"]?.toLongOrNull()

                val docs =
                    transaction {
                        val query = Documents.selectAll()

                        if (contractIdParam != null) {
                            query.andWhere { (Documents.entityType eq "contract") and (Documents.entityId eq contractIdParam) }
                        }
                        if (ticketIdParam != null) {
                            query.andWhere { (Documents.entityType eq "ticket") and (Documents.entityId eq ticketIdParam) }
                        }
                        if (paymentIdParam != null) {
                            query.andWhere { (Documents.entityType eq "payment") and (Documents.entityId eq paymentIdParam) }
                        }

                        // Přístupový filtr se aplikuje v SQL ještě před limitem, aby tenant viděl celý svůj výpis
                        query.andWhere { documentAccessCondition(ctx) }

                        query.limit(200)

                        query.map(::toDocumentResponse)
                    }

                call.respond(docs)
            }

            get("/{id}/pdf") {
                val ctx = call.callerContext()

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

                val isAllowed = checkDocumentAccess(docType, entityType, entityId, ctx.role, ctx.companyId, ctx.userId, ctx.scope)
                if (!isAllowed) {
                    throw NotFoundException("Dokument nenalezen")
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

/**
 * Mapování řádku DB na odpověď dokumentu.
 * @param row řádek výsledku dotazu na dokument
 * @return DTO odpověď dokumentu
 */
private fun toDocumentResponse(row: ResultRow): DocumentResponse =
    DocumentResponse(
        id = row[Documents.id].value,
        type = row[Documents.type],
        entityType = row[Documents.entityType],
        entityId = row[Documents.entityId],
        createdAt = row[Documents.createdAt],
    )

/**
 * Kontrola přístupu k dokumentu podle role a scope. Platformový admin/manager — všechny dokumenty;
 * tenantský admin/manager — dokumenty entit své společnosti; technician — pouze service_report
 * svých tiketů; client — pouze dokumenty entit své společnosti.
 * @param docType typ dokumentu
 * @param entityType typ navázané entity (contract, ticket, payment)
 * @param entityId identifikátor navázané entity
 * @param role role volajícího uživatele
 * @param companyId id společnosti volajícího nebo null
 * @param userId id volajícího uživatele
 * @param scope scope volajícího uživatele (platform/tenant)
 * @return true pokud má uživatel oprávnění k zobrazení dokumentu
 */
private fun checkDocumentAccess(
    docType: DocumentType,
    entityType: String,
    entityId: Long,
    role: String,
    companyId: Long?,
    userId: Long,
    scope: String,
): Boolean {
    if (role == "admin" || role == "manager") {
        if (scope == JwtService.SCOPE_TENANT) {
            // Tenant vidí pouze dokumenty entit své společnosti
            return belongsToCompany(entityType, entityId, companyId)
        }
        // Platformový personál vidí vše
        return true
    }

    if (role == "technician") {
        return docType == DocumentType.service_report &&
            entityType == "ticket" &&
            transaction {
                val ticket = ServiceTickets.selectAll().where { ServiceTickets.id eq entityId }.singleOrNull()
                ticket?.get(ServiceTickets.technicianId)?.value == userId
            }
    }

    if (role == "client") {
        return belongsToCompany(entityType, entityId, companyId)
    }

    return false
}

/**
 * Ověří, zda daná entita (smlouva, tiket nebo platba) náleží zadané společnosti.
 * @param entityType typ entity (contract, ticket, payment)
 * @param entityId id entity
 * @param companyId id společnosti nebo null
 * @return true pokud entita patří zadané společnosti
 */
private fun belongsToCompany(
    entityType: String,
    entityId: Long,
    companyId: Long?,
): Boolean =
    companyId != null &&
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

                else -> false
            }
        }

/**
 * Vytvoří SQL podmínku přístupu k dokumentům pro výpis podle role a scope volajícího.
 * @param ctx kontext volajícího
 * @return SQL podmínka; Op.FALSE znamená prázdný výpis
 */
private fun SqlExpressionBuilder.documentAccessCondition(ctx: CallerContext): Op<Boolean> {
    return when (ctx.role) {
        "admin", "manager" -> {
            if (ctx.scope != JwtService.SCOPE_TENANT) {
                Op.TRUE
            } else {
                val companyId: Long = ctx.companyId ?: return Op.FALSE
                companyDocumentCondition(companyId)
            }
        }

        "client" -> {
            val companyId: Long = ctx.companyId ?: return Op.FALSE
            companyDocumentCondition(companyId)
        }

        "technician" -> {
            val ticketIds: Query =
                ServiceTickets
                    .select(ServiceTickets.id)
                    .where { ServiceTickets.technicianId eq ctx.userId }
            (Documents.type eq DocumentType.service_report) and
                (Documents.entityType eq "ticket") and
                (Documents.entityId inSubQuery ticketIds)
        }

        else -> Op.FALSE
    }
}

/**
 * Vytvoří SQL podmínku pro dokumenty entit patřících zadané společnosti.
 * @param companyId id společnosti
 * @return SQL podmínka pro entity contract/ticket/payment dané společnosti
 */
private fun SqlExpressionBuilder.companyDocumentCondition(companyId: Long): Op<Boolean> {
    val contractIds: Query =
        RentalContracts
            .select(RentalContracts.id)
            .where { RentalContracts.companyId eq companyId }
    val ticketIds: Query =
        ServiceTickets
            .select(ServiceTickets.id)
            .where { ServiceTickets.companyId eq companyId }
    val paymentIds: Query =
        Payments
            .select(Payments.id)
            .where {
                Payments.contractId inSubQuery RentalContracts.select(RentalContracts.id).where { RentalContracts.companyId eq companyId }
            }
    return ((Documents.entityType eq "contract") and (Documents.entityId inSubQuery contractIds)) or
        ((Documents.entityType eq "ticket") and (Documents.entityId inSubQuery ticketIds)) or
        ((Documents.entityType eq "payment") and (Documents.entityId inSubQuery paymentIds))
}
