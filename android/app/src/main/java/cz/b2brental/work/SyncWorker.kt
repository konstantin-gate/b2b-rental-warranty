@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.b2brental.R
import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.domain.util.StatusDiffCalculator
import cz.b2brental.domain.util.TicketChange
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

/**
 * Worker pro periodickou synchronizaci stavů tiketů z backendu a zobrazení notifikací.
 * @param appContext kontext aplikace
 * @param params parametry workeru
 */
public class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        val storage: TokenStorage = GlobalContext.get().get()
        val profile = storage.session.first() ?: return Result.success()

        return try {
            val apiClient: B2bApiClient = GlobalContext.get().get()
            val tickets = apiClient.getTickets()
            val previous: Map<Long, cz.b2brental.domain.model.TicketStatus> =
                StatusDiffCalculator.toStatusMap(tickets)
            val newMap: Map<Long, cz.b2brental.domain.model.TicketStatus> = previous
            // První sync: předáme пустую previousMap, чтобы не генерировать уведомления
            val previousFromPrefs: Map<Long, cz.b2brental.domain.model.TicketStatus> = readStatusMap()
            val changes: List<TicketChange> = StatusDiffCalculator.diff(previousFromPrefs, tickets)
            saveStatusMap(newMap)
            if (changes.isNotEmpty()) {
                showNotification(applicationContext, changes)
            }
            Result.success()
        } catch (_: OfflineException) {
            Result.success()
        } catch (_: ApiException) {
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun readStatusMap(): Map<Long, cz.b2brental.domain.model.TicketStatus> {
        val raw: String = prefs.getString(KEY_STATUS_MAP, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return raw.split(SEP).mapNotNull { token ->
            val parts: List<String> = token.split(SEP_KV)
            if (parts.size != 2) return@mapNotNull null
            val id: Long = parts[0].toLongOrNull() ?: return@mapNotNull null
            val status: cz.b2brental.domain.model.TicketStatus =
                runCatching { cz.b2brental.domain.model.TicketStatus.valueOf(parts[1]) }.getOrNull()
                    ?: return@mapNotNull null
            id to status
        }.toMap()
    }

    private fun saveStatusMap(map: Map<Long, cz.b2brental.domain.model.TicketStatus>) {
        val raw: String = map.entries.joinToString(SEP) { entry -> "${entry.key}${SEP_KV}${entry.value.name}" }
        prefs.edit { putString(KEY_STATUS_MAP, raw) }
    }

    private fun showNotification(context: Context, changes: List<TicketChange>) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted: Int = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }
        val channelId: String = ensureChannel(context)
        val title: String = context.getString(R.string.notification_title)
        val text: String = changes.take(MAX_CHANGES).joinToString("\n") { change: TicketChange ->
            when (change.oldStatus) {
                null -> context.getString(R.string.notification_text_new, change.ticketId, change.newStatus.name)
                else -> context.getString(
                    R.string.notification_text_changed,
                    change.ticketId,
                    change.oldStatus.name,
                    change.newStatus.name,
                )
            }
        }.take(MAX_TEXT)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun ensureChannel(context: Context): String {
        val manager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing: NotificationChannel? = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel: NotificationChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }
        return CHANNEL_ID
    }

    public companion object {
        @Suppress("HardCodedStringLiteral")
        private const val PREFS_NAME: String = "b2b_ticket_sync"
        @Suppress("HardCodedStringLiteral")
        private const val KEY_STATUS_MAP: String = "status_map"
        private const val SEP: String = ";"
        private const val SEP_KV: String = "="
        @Suppress("HardCodedStringLiteral")
        private const val CHANNEL_ID: String = "b2b_service_channel"
        private const val NOTIFICATION_ID: Int = 5001
        private const val MAX_CHANGES: Int = 10
        private const val MAX_TEXT: Int = 250
    }
}
