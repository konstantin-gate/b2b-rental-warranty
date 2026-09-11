@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental.routes

import cz.b2brental.auth.JwtService
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Equipment
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.models.AssistantRequest
import cz.b2brental.models.AssistantResponse
import cz.b2brental.models.DiagnoseRequest
import cz.b2brental.models.DiagnoseResponse
import cz.b2brental.models.WarrantyCheckRequest
import cz.b2brental.models.WarrantyCheckResponse
import cz.b2brental.services.AiService
import cz.b2brental.services.DashboardService
import cz.b2brental.services.KnowledgeBaseService
import cz.b2brental.services.KnowledgeQuery
import cz.b2brental.services.WarrantyEvaluation
import cz.b2brental.services.WarrantyPrelude
import cz.b2brental.services.WarrantyService
import cz.b2brental.services.buildWarrantyPrelude
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ForbiddenException
import cz.b2brental.utils.NotFoundException
import cz.b2brental.utils.requirePlatform
import cz.b2brental.utils.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

/**
 * Registrace tras modulu AI
 * @param aiService služba AI pro diagnostiku, vysvětlení verdiktu a asistenta
 * @param dashboardService služba metrik řídicího panelu pro kontext asistenta
 * @param knowledgeBaseService vyhledávací služba znalostní báze (RAG)
 */
public fun Route.aiRoutes(
    aiService: AiService,
    dashboardService: DashboardService,
    knowledgeBaseService: KnowledgeBaseService,
) {
    route("/ai") {
        authenticate("auth-jwt") {
            post("/diagnose") {
                call.requireRole("client", "manager", "admin")
                val req = call.receive<DiagnoseRequest>()
                if (req.description.isBlank()) {
                    throw BadRequestException("Popis nesmí být prázdný")
                }
                if (req.description.length > 24_000) {
                    throw BadRequestException("Popis musí mít max. 24 000 znaků")
                }
                req.photoBase64?.let { photo ->
                    val decoded: ByteArray =
                        try {
                            java.util.Base64
                                .getDecoder()
                                .decode(photo)
                        } catch (_: IllegalArgumentException) {
                            throw BadRequestException("Fotografie není platný Base64 řetězec")
                        }
                    if (decoded.size > 4 * 1024 * 1024) {
                        throw BadRequestException("Fotografie je příliš velká (max. 4 MB)")
                    }
                }
                val kb =
                    knowledgeBaseService.retrieve(
                        KnowledgeQuery(
                            text = req.description,
                            categoryName = null,
                            equipmentModel = null,
                            errorCode = null,
                        ),
                    )
                val result = aiService.diagnose(req.description, req.photoBase64, kb)
                call.respond(
                    HttpStatusCode.OK,
                    DiagnoseResponse(result.possibleCause, result.severity, result.recommendation),
                )
            }

            post("/warranty-check") {
                call.requireRole("manager", "admin", "technician")
                val req = call.receive<WarrantyCheckRequest>()
                // Kontext volajícího (role, companyId, userId, scope) z JWT tokenu
                val ctx = call.callerContext()
                // Tenantský uživatel bez firmy nemá přístup (403) před transakcí
                val tenantCompanyId: Long? =
                    if (ctx.scope == JwtService.SCOPE_TENANT) {
                        ctx.companyId ?: throw ForbiddenException("Uživatel nemá přiřazenou společnost")
                    } else {
                        null
                    }

                val prelude: WarrantyPrelude =
                    transaction {
                        // Ověření přístupu k vybavení podle role a scope
                        val eqRow =
                            Equipment.selectAll().where { Equipment.id eq req.equipmentId.value }.singleOrNull()
                                ?: throw NotFoundException("Vybavení nenalezeno")

                        when (ctx.role) {
                            "technician" -> {
                                // Technik: vybavení musí být v tiketu přiřazeném právě jemu (userId, ne Users.id)
                                val hasAccess =
                                    ServiceTickets
                                        .selectAll()
                                        .where {
                                            (ServiceTickets.equipmentId eq req.equipmentId.value) and
                                                (ServiceTickets.technicianId eq ctx.userId)
                                        }.firstOrNull() != null
                                if (!hasAccess) throw NotFoundException("Vybavení nenalezeno")
                            }

                            "manager", "admin" -> {
                                if (tenantCompanyId != null) {
                                    // Tenant: vybavení musí být v aktivní smlouvě jeho firmy
                                    val hasAccess =
                                        (ContractItems innerJoin RentalContracts)
                                            .selectAll()
                                            .where {
                                                (ContractItems.equipmentId eq req.equipmentId.value) and
                                                    (RentalContracts.status eq ContractStatus.active) and
                                                    (RentalContracts.companyId eq tenantCompanyId)
                                            }.firstOrNull() != null
                                    if (!hasAccess) throw NotFoundException("Vybavení nenalezeno")
                                }
                                // Platformový manager/admin — bez filtru
                            }
                        }

                        val activeContract =
                            (ContractItems innerJoin RentalContracts)
                                .selectAll()
                                .where {
                                    (ContractItems.equipmentId eq req.equipmentId.value) and
                                        (RentalContracts.status eq ContractStatus.active)
                                }.firstOrNull()

                        buildWarrantyPrelude(
                            categoryId = eqRow[Equipment.categoryId].value,
                            equipmentModel = eqRow[Equipment.model],
                            contractStartDate = activeContract?.get(RentalContracts.startDate),
                        )
                    }

                val evaluation: WarrantyEvaluation =
                    if (!prelude.hasRule) {
                        WarrantyEvaluation(WarrantyVerdict.review_required, "Není záruční pravidlo pro kategorii")
                    } else {
                        WarrantyService.evaluate(
                            contractStartDate = prelude.contractStartDate,
                            warrantyMonths = prelude.warrantyMonths,
                            today = LocalDate.now(),
                        )
                    }

                val kb =
                    knowledgeBaseService.retrieve(
                        KnowledgeQuery(
                            text = req.description ?: "",
                            categoryName = prelude.categoryName,
                            equipmentModel = prelude.equipmentModel,
                            errorCode = null,
                        ),
                    )
                val explanation = aiService.explainVerdict(evaluation.verdict, evaluation.reason, kb)
                call.respond(
                    HttpStatusCode.OK,
                    WarrantyCheckResponse(evaluation.verdict, evaluation.reason, explanation),
                )
            }

            post("/assistant") {
                call.requirePlatform("manager", "admin")
                val req = call.receive<AssistantRequest>()
                if (req.message.isBlank()) {
                    throw BadRequestException("Zpráva nesmí být prázdná")
                }
                if (req.message.length > 2_000) {
                    throw BadRequestException("Zpráva musí mít max. 2 000 znaků")
                }
                val kb =
                    knowledgeBaseService.retrieve(
                        KnowledgeQuery(
                            text = req.message,
                            categoryName = null,
                            equipmentModel = null,
                            errorCode = null,
                        ),
                    )
                val reply = aiService.assistantReply(req.message, dashboardService.metricsAsContext(), kb)
                call.respond(HttpStatusCode.OK, AssistantResponse(reply))
            }
        }
    }
}
