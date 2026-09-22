package com.familyguard.app.work

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyguard.app.ServiceLocator
import com.familyguard.app.location.LocationForegroundService
import com.familyguard.app.util.PermissionUtils

/**
 * Runs once after boot (enqueued by [com.familyguard.app.boot.BootReceiver]). A
 * BroadcastReceiver can't safely do network calls or long-running work directly, so it hands
 * off here. Re-registers geofences (the OS clears them on every reboot) and restarts
 * continuous location sharing — but only if the device is still paired and the relevant
 * permission is still actually granted; neither is ever assumed.
 */
class BootResyncWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val storage = ServiceLocator.secureStorage
        if (!storage.isPaired()) return Result.success()

        if (ServiceLocator.geofenceManager.canRegisterGeofences()) {
            val geofences = ServiceLocator.geofenceRepository.fetchGeofences().getOrNull()
            if (!geofences.isNullOrEmpty()) {
                ServiceLocator.geofenceManager.registerGeofences(geofences)
            }
        }

        if (PermissionUtils.hasBackgroundLocation(applicationContext)) {
            val intent = Intent(applicationContext, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(applicationContext, intent)
            } else {
                applicationContext.startService(intent)
            }
        }

        return Result.success()
    }
}
