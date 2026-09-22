package com.familyguard.app

import android.content.Context
import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.data.remote.ApiService
import com.familyguard.app.data.remote.AuthInterceptor
import com.familyguard.app.data.remote.NetworkFactory
import com.familyguard.app.data.remote.SessionManager
import com.familyguard.app.data.repository.DeviceRepository
import com.familyguard.app.data.repository.GeofenceRepository
import com.familyguard.app.data.repository.LocationRepository
import com.familyguard.app.data.repository.PairingRepository
import com.familyguard.app.data.repository.UsageRepository
import com.familyguard.app.geofence.GeofenceManager
import com.familyguard.app.location.LocationHelper

/**
 * A single, manual composition root for the app's dependencies — no Hilt/Dagger. Everything is
 * created lazily and lives for the process lifetime, which is appropriate here since this app
 * has no per-user multi-account switching.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }

    val secureStorage: SecureStorage by lazy { SecureStorage(appContext) }

    val sessionManager: SessionManager by lazy { SessionManager(secureStorage, appContext) }

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(secureStorage, sessionManager)
    }

    val apiService: ApiService by lazy { NetworkFactory.createApiService(authInterceptor) }

    val locationHelper: LocationHelper by lazy { LocationHelper(appContext) }

    val geofenceManager: GeofenceManager by lazy { GeofenceManager(appContext) }

    private val appVersionName: String by lazy {
        try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
                ?: "unknown"
        } catch (t: Throwable) {
            "unknown"
        }
    }

    val pairingRepository: PairingRepository by lazy {
        PairingRepository(apiService, secureStorage, appVersionName, appContext)
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(apiService, secureStorage, appVersionName)
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(apiService, secureStorage)
    }

    val geofenceRepository: GeofenceRepository by lazy {
        GeofenceRepository(apiService, secureStorage)
    }

    val usageRepository: UsageRepository by lazy {
        UsageRepository(apiService, secureStorage)
    }

    fun appContext(): Context = appContext
}
