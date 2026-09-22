package com.familyguard.app.data.repository

import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.data.remote.ApiService
import com.familyguard.app.data.remote.dto.GeofenceEventRequest
import com.familyguard.app.domain.model.GeofenceDefinition
import java.time.Instant

class GeofenceRepository(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage
) {

    suspend fun fetchGeofences(): Result<List<GeofenceDefinition>> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.getGeofences(deviceId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    body.geofences.map {
                        GeofenceDefinition(it.id, it.name, it.lat, it.lng, it.radiusM)
                    }
                )
            } else {
                Result.failure(IllegalStateException("Request failed with ${response.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun reportGeofenceEvent(
        geofenceId: String,
        transition: String,
        lat: Double,
        lng: Double,
        occurredAt: Instant = Instant.now()
    ): Result<Unit> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.reportGeofenceEvent(
                deviceId,
                GeofenceEventRequest(
                    geofenceId = geofenceId,
                    transition = transition,
                    occurredAt = occurredAt.toString(),
                    lat = lat,
                    lng = lng
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
