package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.domain.model.NotificationItem
import cz.b2brental.domain.repository.NotificationRepository

/**
 * Implementace NotificationRepository — přímé přesměrování na B2bApiClient.
 * @param apiClient HTTP klient pro backend API
 */
public class NotificationRepositoryImpl(
    private val apiClient: B2bApiClient,
) : NotificationRepository {

    /**
     * Načte seznam notifikací (volitelně pouze nepřečtené) a namapuje je do domény.
     * @param unreadOnly true pro vrácení pouze nepřečtených notifikací
     * @return seznam notifikací
     */
    @Suppress("KDocMissingDocumentation")
    override suspend fun getNotifications(unreadOnly: Boolean): List<NotificationItem> {
        return apiClient.getNotifications(unreadOnly).map { dto ->
            NotificationItem(
                id = dto.id,
                message = dto.message,
                isRead = dto.isRead,
                createdAt = dto.createdAt,
            )
        }
    }

    /**
     * Získá počet nepřečtených notifikací.
     * @return počet nepřečtených notifikací
     */
    override suspend fun getUnreadCount(): Int {
        return apiClient.getUnreadNotificationCount().count.toInt()
    }

    /**
     * Označí notifikaci jako přečtenou.
     * @param id ID notifikace
     */
    override suspend fun markRead(id: Long) {
        apiClient.markNotificationRead(id)
    }

    /**
     * Označí všechny notifikace jako přečtené.
     * @return nový počet nepřečtených notifikací (vždy 0)
     */
    override suspend fun markAllRead(): Int {
        return apiClient.markAllNotificationsRead().count.toInt()
    }
}
