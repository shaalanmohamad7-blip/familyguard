package com.familyguard.app.location

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.pm.ServiceInfoCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.familyguard.app.ServiceLocator
import com.familyguard.app.util.Constants
import com.familyguard.app.util.NotificationHelper
import com.familyguard.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * User-initiated location sharing, run as a foreground service (foregroundServiceType
 * "location") with a persistent, plainly-worded notification while it is active. This is the
 * same Play Store "user-initiated data transfer" style category Google allows for Find-My-
 * Friend-style sharing apps.
 *
 * This service is only ever started when the device is paired AND both foreground and
 * background location are granted. Geofencing is handled entirely separately by
 * [com.familyguard.app.geofence.GeofenceManager] — never through this service — per Google's
 * 2027-01-27 Play policy change that removes geofencing as a foreground-service justification.
 */
class LocationForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ServiceLocator.secureStorage.isPaired() || !PermissionUtils.hasBackgroundLocation(this)) {
            // Never report without both an active pairing and an active, user-granted permission.
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationHelper.buildLocationSharingNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                Constants.NOTIF_ID_LOCATION_SHARING,
                notification,
                ServiceInfoCompat.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(Constants.NOTIF_ID_LOCATION_SHARING, notification)
        }

        startLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationCallback != null) return // Already running.

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch {
                    ServiceLocator.locationRepository.reportLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        accuracyM = location.accuracy,
                        source = Constants.SOURCE_BACKGROUND
                    )
                }
            }
        }
        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(request, callback, mainLooper)
    }

    override fun onDestroy() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
