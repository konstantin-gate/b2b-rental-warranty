package cz.b2brental.domain.repository

import cz.b2brental.domain.model.NotificationItem

/**
 * Rozhraní repozitáře pro správu notifikací uživatele.
 */
public interface NotificationRepository {

    /**
     * Načte seznam notifikací přihlášeného uživatele.
     * @param unreadOnly true pro vrácení pouze nepřečtených notifikací
     * @return seznam notifikací
     */
    public suspend fun getNotifications(unreadOnly: Boolean): List<NotificationItem>

    /**
     * Získá počet nepřečtených notifikací.
     * @return počet nepřečtených notifikací
     */
    public suspend fun getUnreadCount(): Int

    /**
     * Označí notifikaci jako přečtenou.
     * @param id ID notifikace
     */
    public suspend fun markRead(id: Long)

    /**
     * Označí všechny notifikace jako přečtené.
     * @return nový počet nepřečtených notifikací (vždy 0)
     */
    public suspend fun markAllRead(): Int
}
