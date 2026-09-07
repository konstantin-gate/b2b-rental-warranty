@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Notifications
import cz.b2brental.db.Users
import cz.b2brental.models.NotificationResponse
import cz.b2brental.models.UnreadCountResponse
import cz.b2brental.utils.NotFoundException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Služba notifikací: jediný zdroj notifikací je backend.
 * Zapisuje notifikace do tabulky Notifications a poskytuje čtení a označení přečtení.
 */
public class NotificationService {
    /**
     * Vytvoří notifikace pro všechny uživatele rolí manager a admin.
     * Musí být voláno uvnitř externí transakce (nevytváří vlastní transaction {}).
     * @param message text notifikace
     */
    @Suppress("KDocMissingDocumentation")
    public fun notifyManagersAndAdmins(message: String) {
        val managers: List<EntityID<Long>> =
            Users
                .selectAll()
                .where { Users.role inList listOf("manager", "admin") }
                .map { row -> row[Users.id] }
        for (manager in managers) {
            Notifications.insert {
                it[userId] = manager
                it[Notifications.message] = message
            }
        }
    }

    /**
     * Vytvoří notifikace pro všechny klienty dané firmy.
     * Musí být voláno uvnitř externí transakce (nevytváří vlastní transaction {}).
     * @param companyId identifikátor firmy
     * @param message text notifikace
     */
    @Suppress("KDocMissingDocumentation")
    public fun notifyCompanyClients(
        companyId: Long,
        message: String,
    ) {
        val clients: List<EntityID<Long>> =
            Users
                .selectAll()
                .where { (Users.companyId eq companyId) and (Users.role eq "client") }
                .map { row -> row[Users.id] }
        for (client in clients) {
            Notifications.insert {
                it[userId] = client
                it[Notifications.message] = message
            }
        }
    }

    /**
     * Vytvoří notifikaci pro jednoho technika.
     * Musí být voláno uvnitř externí transakce (nevytváří vlastní transaction {}).
     * @param technicianId identifikátor technika
     * @param message text notifikace
     */
    public fun notifyTechnician(
        technicianId: Long,
        message: String,
    ) {
        Notifications.insert {
            it[userId] = EntityID(technicianId, Users)
            it[Notifications.message] = message
        }
    }

    /**
     * Vrátí seznam notifikací uživatele (nejnovější první, maximálně 100).
     * @param userId identifikátor uživatele
     * @param onlyUnread true pro vrácení pouze nepřečtených notifikací
     * @return seznam notifikací
     */
    @Suppress("KDocMissingDocumentation")
    public fun list(
        userId: Long,
        onlyUnread: Boolean,
    ): List<NotificationResponse> =
        transaction {
            val query = Notifications.selectAll().where { Notifications.userId eq userId }
            if (onlyUnread) {
                query.andWhere { Notifications.isRead eq false }
            }
            query
                .orderBy(Notifications.createdAt to SortOrder.DESC, Notifications.id to SortOrder.DESC)
                .limit(100)
                .map { row ->
                    NotificationResponse(
                        id = row[Notifications.id].value,
                        message = row[Notifications.message],
                        isRead = row[Notifications.isRead],
                        createdAt = row[Notifications.createdAt],
                    )
                }
        }

    /**
     * Vrátí počet nepřečtených notifikací uživatele.
     * @param userId identifikátor uživatele
     * @return počet nepřečtených notifikací
     */
    public fun unreadCount(userId: Long): Long =
        transaction {
            Notifications
                .selectAll()
                .where { (Notifications.userId eq userId) and (Notifications.isRead eq false) }
                .count()
        }

    /**
     * Označí notifikaci jako přečтенou. Cizí nebo neexistující notifikace → NotFoundException.
     * @param userId identifikátor uživatele (vlastník notifikace)
     * @param notificationId identifikátor notifikace
     * @return aktualizovaná notifikace
     */
    public fun markRead(
        userId: Long,
        notificationId: Long,
    ): NotificationResponse =
        transaction {
            val updated: Int =
                Notifications.update({ (Notifications.id eq notificationId) and (Notifications.userId eq userId) }) {
                    it[isRead] = true
                }
            if (updated == 0) {
                throw NotFoundException("Notifikace nenalezena")
            }
            val row: ResultRow =
                Notifications
                    .selectAll()
                    .where { Notifications.id eq notificationId }
                    .single()
            NotificationResponse(
                id = row[Notifications.id].value,
                message = row[Notifications.message],
                isRead = row[Notifications.isRead],
                createdAt = row[Notifications.createdAt],
            )
        }

    /**
     * Označí všechny notifikace uživatele jako přečtené.
     * @param userId identifikátor uživatele
     * @return odpověď s nulovým počtem nepřečtených
     */
    public fun markAllRead(userId: Long): UnreadCountResponse =
        transaction {
            Notifications.update({ (Notifications.userId eq userId) and (Notifications.isRead eq false) }) {
                it[isRead] = true
            }
            UnreadCountResponse(0L)
        }
}
