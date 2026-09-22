package com.familyguard.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.familyguard.app.domain.model.UsageRecord
import com.familyguard.app.util.PermissionUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Reads coarse, per-app usage minutes for a single day from UsageStatsManager. This never
 * reads message content, screenshots, or anything beyond the OS's own aggregate "how long was
 * this app in the foreground" totals — and only runs at all when the user has granted Usage
 * Access in system Settings.
 */
class UsageStatsHelper(private val context: Context) {

    /** Aggregates usage for the previous full calendar day (used by the daily worker). */
    fun previousDayUsage(): Pair<String, List<UsageRecord>> {
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateLabel = isoDate(end.timeInMillis - TimeUnit.DAYS.toMillis(1))
        val start = end.timeInMillis - TimeUnit.DAYS.toMillis(1)
        return dateLabel to usageBetween(start, end.timeInMillis)
    }

    fun usageBetween(startMillis: Long, endMillis: Long): List<UsageRecord> {
        if (!PermissionUtils.hasUsageAccess(context)) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startMillis,
            endMillis
        ) ?: return emptyList()

        val packageManager = context.packageManager

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .map { (packageName, entries) ->
                val totalMs = entries.sumOf { it.totalTimeInForeground }
                UsageRecord(
                    appPackage = packageName,
                    appLabel = appLabelFor(packageManager, packageName),
                    minutes = TimeUnit.MILLISECONDS.toMinutes(totalMs)
                )
            }
            .filter { it.minutes > 0 }
    }

    private fun appLabelFor(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (t: Throwable) {
            packageName
        }
    }

    private fun isoDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }
}
