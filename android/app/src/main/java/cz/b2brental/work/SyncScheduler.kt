@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Plánovač periodické synchronizace stavů hlášení (WorkManager). */
public object SyncScheduler {

    /** Název jedinečné periodické práce. */
    @Suppress("HardCodedStringLiteral")
    private const val WORK_NAME: String = "b2b_ticket_sync"

    /**
     * Naplánuje periodickou synchronizaci každých 15 minut při připojení k síti.
     * Opakované volání je bezpečné (politika KEEP).
     * @param context kontext aplikace
     */
    public fun scheduleSync(context: Context): Unit {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
