package com.familyguard.app.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.familyguard.app.domain.model.GeofenceDefinition
import com.familyguard.app.util.PermissionUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps the standalone GeofencingClient API directly (never routed through the foreground
 * location service). Google's Play policy change effective 2027-01-27 removes geofencing as a
 * justification for a location foreground service, so this is built the compliant way from
 * day one: geofence registration is independent of whether continuous sharing is running.
 */
class GeofenceManager(private val context: Context) {

    private val client: GeofencingClient by lazy { LocationServices.getGeofencingClient(context) }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        PendingIntent.getBroadcast(context, GEOFENCE_REQUEST_CODE, intent, flags)
    }

    /** Geofence transitions are only reliable in the background with background location granted. */
    fun canRegisterGeofences(): Boolean = PermissionUtils.hasBackgroundLocation(context)

    @SuppressLint("MissingPermission")
    suspend fun registerGeofences(definitions: List<GeofenceDefinition>): Boolean {
        if (!canRegisterGeofences() || definitions.isEmpty()) return false

        val geofences = definitions.map { def ->
            Geofence.Builder()
                .setRequestId(def.id)
                .setCircularRegion(def.latitude, def.longitude, def.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        return suspendCancellableCoroutine { continuation ->
            client.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(true) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(false) }
        }
    }

    suspend fun clearGeofences() {
        suspendCancellableCoroutine<Unit> { continuation ->
            client.removeGeofences(geofencePendingIntent)
                .addOnCompleteListener { if (continuation.isActive) continuation.resume(Unit) }
        }
    }

    companion object {
        private const val GEOFENCE_REQUEST_CODE = 5001
    }
}
