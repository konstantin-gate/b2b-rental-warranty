@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.domain.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Odpověď s údaji o notifikaci uživatele
 * @property id identifikátor notifikace
 * @property message zpráva notifikace
 * @property isRead příznak přečtení
 * @property createdAt čas vytvoření notifikace
 */
@Serializable
public data class NotificationResponse(
    public val id: Long,
    public val message: String,
    public val isRead: Boolean,
    @Serializable(with = InstantSerializer::class) public val createdAt: Instant,
)

/**
 * Odpověď s počtem nepřečtených notifikací
 * @property count počet nepřečtených notifikací
 */
@Serializable
public data class UnreadCountResponse(
    public val count: Long,
)
