package com.familyguard.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.familyguard.app.util.PermissionUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LocationFix(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float
)

/**
 * Thin wrapper around FusedLocationProviderClient. Every call here checks permission first and
 * returns null rather than throwing when it isn't granted — callers show that as "location
 * unavailable" rather than crashing or silently guessing a location.
 */
class LocationHelper(private val context: Context) {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * One-time precise fix, used for SOS and the voluntary check-in. This only needs
     * foreground/precise location — never the broader background permission — which is why
     * SOS still works for a child who granted foreground location but skipped background
     * sharing.
     */
    @SuppressLint("MissingPermission")
    suspend fun getOneTimeLocation(): LocationFix? {
        if (!PermissionUtils.hasForegroundLocation(context)) return null

        val request = CurrentLocationRequest.Builder()
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setDurationMillis(20_000L)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val cancellationSource = com.google.android.gms.tasks.CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationSource.cancel() }

            client.getCurrentLocation(request, cancellationSource.token)
                .addOnSuccessListener { location ->
                    val fix = location?.let { LocationFix(it.latitude, it.longitude, it.accuracy) }
                    if (continuation.isActive) continuation.resume(fix)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    /** Cheaper, may be slightly stale — used by the background service's periodic reports. */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationFix? {
        if (!PermissionUtils.hasForegroundLocation(context)) return null

        return suspendCancellableCoroutine { continuation ->
            client.lastLocation
                .addOnSuccessListener { location ->
                    val fix = location?.let { LocationFix(it.latitude, it.longitude, it.accuracy) }
                    if (continuation.isActive) continuation.resume(fix)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }
}
