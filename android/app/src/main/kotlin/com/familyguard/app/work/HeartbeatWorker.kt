package com.familyguard.app.work

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyguard.app.ServiceLocator
import com.familyguard.app.location.LocationForegroundService
import com.familyguard.app.util.BatteryUtils
import com.familyguard.app.util.Constants
import com.familyguard.app.util.PermissionUtils

/**
 * Periodic, battery-friendly fallback that runs regardless of whether the foreground service
 * is alive. Each run:
 *  1. Reports a status heartbeat (battery %, current permission snapshot, app version) so the
 *     dashboard always has a recent "last seen" even if location sharing is off.
 *  2. If background location is granted, fetches one lightweight last-known-location fix as a
 *     fallback data point, in case the OS or an OEM battery manager killed the foreground
 *     service since the last heartbeat.
 *  3. If continuous sharing should be running (paired + background location granted) but the
 *     foreground service isn't, restarts it.
 */
class HeartbeatWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val storage = ServiceLocator.secureStorage
        if (!storage.isPaired()) return Result.success()

        val snapshot = PermissionUtils.currentSnapshot(applicationContext)
        val battery = BatteryUtils.currentBatteryPercent(applicationContext)
        ServiceLocator.deviceRepository.sendHeartbeat(battery, snapshot)

        if (snapshot.canShareLocationContinuously) {
            ServiceLocator.locationHelper.getLastKnownLocation()?.let { fix ->
                ServiceLocator.locationRepository.reportLocation(
                    lat = fix.lat,
                    lng = fix.lng,
                    accuracyM = fix.accuracyM,
                    source = Constants.SOURCE_BACKGROUND
                )
            }
            ensureForegroundServiceRunning()
        }

        return Result.success()
    }

    private fun ensureForegroundServiceRunning() {
        val intent = Intent(applicationContext, LocationForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(applicationContext, intent)
        } else {
            applicationContext.startService(intent)
        }
    }
}
