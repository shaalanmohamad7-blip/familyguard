package com.familyguard.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyguard.app.ServiceLocator
import com.familyguard.app.util.PermissionUtils
import com.familyguard.app.work.WorkScheduler

/**
 * After a reboot, resumes background reporting — but only if both conditions still hold:
 * the device is still paired, and the relevant permission is still granted. Neither is ever
 * assumed; both are re-checked live before anything is restarted.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        ServiceLocator.init(context)
        val storage = ServiceLocator.secureStorage
        if (!storage.isPaired()) return

        // Periodic WorkManager jobs already persist across reboot on their own, but we
        // re-arm them defensively and use a one-time worker for anything that needs a fresh
        // permission check plus network access (geofence re-registration, restarting the
        // foreground service).
        WorkScheduler.scheduleBackgroundWorkIfPaired(context)

        if (PermissionUtils.hasBackgroundLocation(context) || ServiceLocator.geofenceManager.canRegisterGeofences()) {
            WorkScheduler.scheduleBootResync(context)
        }
    }
}
