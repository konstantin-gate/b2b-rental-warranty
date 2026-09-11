@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.db.DocumentType
import cz.b2brental.db.Documents
import cz.b2brental.db.HistoryEvents
import cz.b2brental.db.Users
import cz.b2brental.utils.login
import cz.b2brental.utils.withB2bTestApp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TicketFlowTest {
    @Test
    fun ticketLifecycleFlow(): Unit =
        withB2bTestApp("ticket-flow") {
            val clientToken = login("kitchen@b2b.demo", "kitchen1234abcd")
            val managerToken = login("manager@b2b.demo", "manager1234abcd")
            val techToken = login("tech@b2b.demo", "tech12345abcd")

            val techId =
                transaction {
                    Users
                        .selectAll()
                        .where { Users.role eq "technician" }
                        .first()[Users.id]
                        .value
                }

            val createResp =
                client.post("/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":2,\"description\":\"Kompresor hučí a nechladí dostatečně\"}")
                }
            assertEquals(HttpStatusCode.Created, createResp.status)
            val text = createResp.bodyAsText()
            val match = Regex("\"id\":(\\d+)").find(text) ?: error("Nenalezeno id tiketu: $text")
            val ticketId = match.groupValues[1].toLong()

            val assignResp =
                client.post("/tickets/$ticketId/assign") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"technicianId\":$techId}")
                }
            assertEquals(HttpStatusCode.OK, assignResp.status)

            val startResp =
                client.post("/tickets/$ticketId/start") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.OK, startResp.status)

            val resolveResp =
                client.post("/tickets/$ticketId/resolve") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"result\":\"repaired\",\"notes\":\"Vyměněn ventilátor chlazení\"}")
                }
            assertEquals(HttpStatusCode.OK, resolveResp.status)

            transaction {
                val doc =
                    Documents
                        .selectAll()
                        .where {
                            (Documents.type eq DocumentType.service_report) and (Documents.entityId eq ticketId)
                        }.singleOrNull()
                assertTrue(doc != null, "Servisní zpráva musí být vytvořena v tabulce documents")

                val events =
                    HistoryEvents
                        .selectAll()
                        .where {
                            (HistoryEvents.entityType eq "ticket") and (HistoryEvents.entityId eq ticketId)
                        }.map { it[HistoryEvents.eventType] }
                assertTrue("ticket_created" in events)
                assertTrue("ticket_resolved" in events)
            }
        }
}
