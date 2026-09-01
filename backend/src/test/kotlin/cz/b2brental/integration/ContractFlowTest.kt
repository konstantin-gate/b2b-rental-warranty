@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class ContractFlowTest {
    @Test
    fun contractApproveRejectFlow(): Unit =
        withB2bTestApp("contract-flow") {
            val clientToken = login("kitchen@b2b.demo", "kitchen123")
            val managerToken = login("manager@b2b.demo", "manager123")
            val techToken = login("tech@b2b.demo", "tech123")

            val createResp =
                client.post("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentIds\":[5,6],\"months\":6,\"startDate\":\"${LocalDate.now()}\",\"deliveryAddress\":\"Praha 5\"}")
                }
            assertEquals(HttpStatusCode.Created, createResp.status)
            val text = createResp.bodyAsText()
            val match = Regex("\"id\":(\\d+)").find(text) ?: error("Nenalezeno id smlouvy: $text")
            val contractId = match.groupValues[1].toLong()

            val approveResp =
                client.post("/contracts/$contractId/approve") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, approveResp.status)

            transaction {
                val c = RentalContracts.selectAll().where { RentalContracts.id eq contractId }.single()
                assertEquals(ContractStatus.active, c[RentalContracts.status])

                val paymentsCount = Payments.selectAll().where { Payments.contractId eq contractId }.count()
                assertEquals(6, paymentsCount)

                val eq5 = Equipment.selectAll().where { Equipment.id eq 5L }.single()
                val eq6 = Equipment.selectAll().where { Equipment.id eq 6L }.single()
                assertEquals(EquipmentStatus.rented, eq5[Equipment.status])
                assertEquals(EquipmentStatus.rented, eq6[Equipment.status])
            }

            val repeatApprove =
                client.post("/contracts/$contractId/approve") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.Conflict, repeatApprove.status)

            val techContracts =
                client.get("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.Forbidden, techContracts.status)
        }
}
