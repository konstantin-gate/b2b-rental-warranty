@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.integration

import cz.b2brental.db.ContractStatus
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.Users
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationFlowTest {
    @Test
    fun notificationLifecycleFlow(): Unit =
        withB2bTestApp("notification-flow") {
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

            // 1. Vytvoření tiketu klientem → notifikace pro manažery a adminy
            val createResp =
                client.post("/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentId\":2,\"description\":\"Kompresor hučí a nechladí dostatečně\"}")
                }
            assertEquals(HttpStatusCode.Created, createResp.status)
            val createText = createResp.bodyAsText()
            val ticketId =
                Regex("\"id\":(\\d+)")
                    .find(createText)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezeno id tiketu: $createText")

            val managerList =
                client.get("/notifications") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, managerList.status)
            val managerListText = managerList.bodyAsText()
            assertTrue("Nový servisní tiket #$ticketId" in managerListText)
            assertTrue("\"isRead\":false" in managerListText)

            val managerCountResp =
                client.get("/notifications/unread-count") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, managerCountResp.status)
            val managerCountText = managerCountResp.bodyAsText()
            val managerCount =
                Regex("\"count\":(\\d+)")
                    .find(managerCountText)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezen count: $managerCountText")
            assertTrue(managerCount > 0)

            // 2. Přiřazení technika → notifikace technikovi
            val assignResp =
                client.post("/tickets/$ticketId/assign") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"technicianId\":$techId}")
                }
            assertEquals(HttpStatusCode.OK, assignResp.status)

            val techUnread =
                client.get("/notifications?unread=true") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.OK, techUnread.status)
            val techUnreadText = techUnread.bodyAsText()
            assertTrue("vám byl přiřazen" in techUnreadText)
            val foreignId =
                Regex("\"id\":(\\d+)")
                    .find(techUnreadText)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezeno id notifikace technika: $techUnreadText")

            // 3. Zahájení prací technikem → notifikace manažerům
            val startResp =
                client.post("/tickets/$ticketId/start") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                }
            assertEquals(HttpStatusCode.OK, startResp.status)

            val managerUnreadAfterStart =
                client.get("/notifications?unread=true") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, managerUnreadAfterStart.status)
            assertTrue("zahájil práce" in managerUnreadAfterStart.bodyAsText())

            // 4. Vyřešení tiketu technikem → notifikace manažerům i klientům firmy
            val resolveResp =
                client.post("/tickets/$ticketId/resolve") {
                    header(HttpHeaders.Authorization, "Bearer $techToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"result\":\"repaired\",\"notes\":\"Vyměněn ventilátor\"}")
                }
            assertEquals(HttpStatusCode.OK, resolveResp.status)

            val clientList =
                client.get("/notifications") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, clientList.status)
            val clientListText = clientList.bodyAsText()
            assertTrue("vyřešen (repaired)" in clientListText)

            // 5. Označení vlastní notifikace jako přečtené, počet nepřečtených klesne
            val countBeforeResp =
                client.get("/notifications/unread-count") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, countBeforeResp.status)
            val countBefore =
                Regex("\"count\":(\\d+)")
                    .find(countBeforeResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezen count klienta")
            assertTrue(countBefore > 0)

            val ownId =
                Regex("\"id\":(\\d+)")
                    .find(clientListText)
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezeno id notifikace klienta: $clientListText")
            val markReadResp =
                client.post("/notifications/$ownId/read") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, markReadResp.status)

            val countAfterResp =
                client.get("/notifications/unread-count") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, countAfterResp.status)
            val countAfter =
                Regex("\"count\":(\\d+)")
                    .find(countAfterResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezen count klienta po přečtení")
            assertEquals(countBefore - 1, countAfter)

            // 6. Cizí notifikace (notifikace technika) → 404
            val foreignResp =
                client.post("/notifications/$foreignId/read") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.NotFound, foreignResp.status)

            // 7. Označení všech notifikací jako přečtených
            val markAllResp =
                client.post("/notifications/read-all") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, markAllResp.status)
            assertEquals("{\"count\":0}", markAllResp.bodyAsText())

            val clientUnreadAfterAll =
                client.get("/notifications?unread=true") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, clientUnreadAfterAll.status)
            assertFalse("\"isRead\":false" in clientUnreadAfterAll.bodyAsText())

            // 8. Schválení smlouvy → notifikace klientům firmy
            val createContractResp =
                client.post("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentIds\":[5,6],\"months\":6,\"startDate\":\"${LocalDate.now()}\",\"deliveryAddress\":\"Praha 5\"}")
                }
            assertEquals(HttpStatusCode.Created, createContractResp.status)
            val contractId =
                Regex("\"id\":(\\d+)")
                    .find(createContractResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezeno id smlouvy")

            val approveResp =
                client.post("/contracts/$contractId/approve") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, approveResp.status)

            val clientListAfterApprove =
                client.get("/notifications") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, clientListAfterApprove.status)
            assertTrue("Smlouva #$contractId byla schválena" in clientListAfterApprove.bodyAsText())

            // 9. Zamítnutí smlouvy → notifikace klientům firmy
            val createRejectedContractResp =
                client.post("/contracts") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"equipmentIds\":[3],\"months\":3,\"startDate\":\"${LocalDate.now()}\",\"deliveryAddress\":\"Brno 1\"}")
                }
            assertEquals(HttpStatusCode.Created, createRejectedContractResp.status)
            val rejectedContractId =
                Regex("\"id\":(\\d+)")
                    .find(createRejectedContractResp.bodyAsText())
                    ?.groupValues
                    ?.get(1)
                    ?.toLong()
                    ?: error("Nenalezeno id zamítané smlouvy")

            val rejectResp =
                client.post("/contracts/$rejectedContractId/reject") {
                    header(HttpHeaders.Authorization, "Bearer $managerToken")
                }
            assertEquals(HttpStatusCode.OK, rejectResp.status)

            val clientListAfterReject =
                client.get("/notifications") {
                    header(HttpHeaders.Authorization, "Bearer $clientToken")
                }
            assertEquals(HttpStatusCode.OK, clientListAfterReject.status)
            assertTrue("Smlouva #$rejectedContractId byla zamítnuta" in clientListAfterReject.bodyAsText())

            val rejectedStatus =
                transaction {
                    RentalContracts
                        .selectAll()
                        .where { RentalContracts.id eq rejectedContractId }
                        .single()[RentalContracts.status]
                }
            assertEquals(ContractStatus.rejected, rejectedStatus)
        }
}
