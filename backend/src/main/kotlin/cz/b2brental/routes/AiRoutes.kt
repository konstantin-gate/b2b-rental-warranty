@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Equipment
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.WarrantyRules
import cz.b2brental.db.WarrantyVerdict
import cz.b2brental.models.AssistantRequest
import cz.b2brental.models.AssistantResponse
import cz.b2brental.models.DiagnoseRequest
import cz.b2brental.models.DiagnoseResponse
import cz.b2brental.models.WarrantyCheckRequest
import cz.b2brental.models.WarrantyCheckResponse
import cz.b2brental.services.AiService
import cz.b2brental.services.DashboardService
import cz.b2brental.services.WarrantyEvaluation
import cz.b2brental.services.WarrantyService
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.NotFoundException
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

/** Registrace tras modulu AI */
public fun Route.aiRoutes(
    aiService: AiService,
    dashboardService: DashboardService,
) {
    route("/ai") {
        authenticate("auth-jwt") {
            post("/diagnose") {
                call.requireRole("client", "manager", "admin")
                val req = call.receive<DiagnoseRequest>()
                if (req.description.isBlank()) {
                    throw BadRequestException("Popis nesmí být prázdný")
                }
                val result = aiService.diagnose(req.description, req.photoBase64)
                call.respond(
                    HttpStatusCode.OK,
                    DiagnoseResponse(result.possibleCause, result.severity, result.recommendation),
                )
            }

            post("/warranty-check") {
                call.requireRole("manager", "admin", "technician")
                val req = call.receive<WarrantyCheckRequest>()

                val (contractStartDate, warrantyRuleRow) =
                    transaction {
                        val eqRow =
                            Equipment.selectAll().where { Equipment.id eq req.equipmentId.value }.singleOrNull()
                                ?: throw NotFoundException("Vybavení nenalezeno")

                        val activeContract =
                            (ContractItems innerJoin RentalContracts)
                                .selectAll()
                                .where {
                                    (ContractItems.equipmentId eq req.equipmentId.value) and
                                        (RentalContracts.status eq ContractStatus.active)
                                }.firstOrNull()

                        val rule =
                            WarrantyRules
                                .selectAll()
                                .where { WarrantyRules.categoryId eq eqRow[Equipment.categoryId].value }
                                .singleOrNull()

                        Pair(activeContract?.get(RentalContracts.startDate), rule)
                    }

                val evaluation: WarrantyEvaluation =
                    if (warrantyRuleRow == null) {
                        WarrantyEvaluation(WarrantyVerdict.review_required, "Není záruční pravidlo pro kategorii")
                    } else {
                        val excludedCauses =
                            warrantyRuleRow[WarrantyRules.excludedCauses]
                                .split(',')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                        WarrantyService.evaluate(
                            contractStartDate = contractStartDate,
                            warrantyMonths = warrantyRuleRow[WarrantyRules.warrantyMonths],
                            description = req.description ?: "",
                            excludedCauses = excludedCauses,
                            today = LocalDate.now(),
                        )
                    }

                val explanation = aiService.explainVerdict(evaluation.verdict, evaluation.reason)
                call.respond(
                    HttpStatusCode.OK,
                    WarrantyCheckResponse(evaluation.verdict, evaluation.reason, explanation),
                )
            }

            post("/assistant") {
                call.requireRole("manager", "admin")
                val req = call.receive<AssistantRequest>()
                if (req.message.isBlank()) {
                    throw BadRequestException("Zpráva nesmí být prázdná")
                }
                val reply = aiService.assistantReply(req.message, dashboardService.metricsAsContext())
                call.respond(HttpStatusCode.OK, AssistantResponse(reply))
            }
        }
    }
}
