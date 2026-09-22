package com.familyguard.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.familyguard.app.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transitionType = geofencingEvent.geofenceTransition
        val transitionName = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> return // Not a transition we report (e.g. DWELL is unused here).
        }

        val triggeringLocation = geofencingEvent.triggeringLocation
        val lat = triggeringLocation?.latitude ?: return
        val lng = triggeringLocation.longitude

        val geofences = geofencingEvent.triggeringGeofences ?: return
        if (geofences.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(15_000L) {
                    geofences.forEach { geofence ->
                        ServiceLocator.geofenceRepository.reportGeofenceEvent(
                            geofenceId = geofence.requestId,
                            transition = transitionName,
                            lat = lat,
                            lng = lng
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
