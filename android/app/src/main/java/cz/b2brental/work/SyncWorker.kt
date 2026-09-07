@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.b2brental.R
import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.domain.model.NotificationItem
import cz.b2brental.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

/**
 * Worker pro periodické načítání nepřečtených notifikací z backendu.
 * Jediným zdrojem notifikací je backend; klient pouze načte GET /notifications?unread=true,
 * zobrazí systémové notifikace a označí vše jako přečtené.
 * @param appContext kontext aplikace
 * @param params parametry workeru
 */
public class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val storage: TokenStorage = GlobalContext.get().get()
        if (storage.session.first() == null) return Result.success()

        val repository: NotificationRepository = GlobalContext.get().get()

        return try {
            val unread: List<NotificationItem> = repository.getNotifications(true)
            if (unread.isNotEmpty()) {
                showNotification(applicationContext, unread)
                repository.markAllRead()
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

    private fun showNotification(context: Context, items: List<NotificationItem>) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted: Int = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }
        val channelId: String = ensureChannel(context)
        val title: String = context.getString(R.string.notification_title)
        val text: String =
            items.take(MAX_ITEMS).joinToString("\n") { item -> item.message }.take(MAX_TEXT)

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
        private const val CHANNEL_ID: String = "b2b_service_channel"
        private const val NOTIFICATION_ID: Int = 5001
        private const val MAX_ITEMS: Int = 5
        private const val MAX_TEXT: Int = 250
    }
}
