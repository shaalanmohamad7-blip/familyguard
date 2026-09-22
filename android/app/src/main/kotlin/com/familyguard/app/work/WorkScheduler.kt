package com.familyguard.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.familyguard.app.ServiceLocator
import com.familyguard.app.usage.UsageReportWorker
import com.familyguard.app.util.Constants
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Called from Application.onCreate and after a successful pairing. No-op if not paired. */
    fun scheduleBackgroundWorkIfPaired(context: Context) {
        if (!ServiceLocator.secureStorage.isPaired()) return
        scheduleHeartbeat(context)
        scheduleUsageReporting(context)
    }

    fun scheduleHeartbeat(context: Context) {
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            Constants.HEARTBEAT_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WORK_HEARTBEAT,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleUsageReporting(context: Context) {
        val request = PeriodicWorkRequestBuilder<UsageReportWorker>(
            Constants.USAGE_REPORT_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WORK_USAGE_REPORT,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleBootResync(context: Context) {
        val request = OneTimeWorkRequestBuilder<BootResyncWorker>()
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORK_BOOT_RESYNC,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Called on unpair: stop every background job immediately. */
    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(Constants.WORK_HEARTBEAT)
        workManager.cancelUniqueWork(Constants.WORK_USAGE_REPORT)
        workManager.cancelUniqueWork(Constants.WORK_BOOT_RESYNC)
    }
}
