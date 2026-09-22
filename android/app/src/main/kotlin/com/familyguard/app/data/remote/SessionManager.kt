package com.familyguard.app.data.remote

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.util.Constants
import com.familyguard.app.util.NotificationHelper
import com.familyguard.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Reacts to the backend telling us this device's credentials are no longer valid (a parent
 * unpaired it, or the token was revoked). We never retry silently forever — instead we clear
 * local state once, post a clear notification (this can happen from a background worker, with
 * no UI visible at all), and surface an in-app event so any visible UI routes back to pairing.
 */
class SessionManager(
    private val secureStorage: SecureStorage,
    private val appContext: Context
) {

    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

    /** Called once per 401/403 response. Idempotent: if already unpaired, this is a no-op. */
    fun onUnauthorized() {
        if (!secureStorage.isPaired()) return
        secureStorage.clearPairing()
        notifyUnpaired()
        _sessionExpiredEvents.tryEmit(Unit)
    }

    private fun notifyUnpaired() {
        if (!PermissionUtils.hasNotificationPermission(appContext)) return
        val notification = NotificationHelper.buildUnpairedNotification(appContext)
        NotificationManagerCompat.from(appContext)
            .notify(Constants.NOTIF_ID_UNPAIRED, notification)
    }
}
