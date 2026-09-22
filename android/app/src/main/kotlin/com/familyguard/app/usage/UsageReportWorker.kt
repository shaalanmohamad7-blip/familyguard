package com.familyguard.app.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyguard.app.ServiceLocator
import com.familyguard.app.util.PermissionUtils

/** Daily job: reads yesterday's per-app usage minutes and posts them, only if Usage Access is granted. */
class UsageReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val storage = ServiceLocator.secureStorage
        if (!storage.isPaired()) return Result.success()
        if (!PermissionUtils.hasUsageAccess(applicationContext)) return Result.success()

        val (date, records) = UsageStatsHelper(applicationContext).previousDayUsage()
        if (records.isEmpty()) return Result.success()

        val result = ServiceLocator.usageRepository.reportUsage(date, records)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
