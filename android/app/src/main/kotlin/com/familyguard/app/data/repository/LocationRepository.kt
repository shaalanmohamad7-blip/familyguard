package com.familyguard.app.data.repository

import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.data.remote.ApiService
import com.familyguard.app.data.remote.dto.LocationReportRequest
import com.familyguard.app.data.remote.dto.SosRequest
import java.time.Instant

class LocationRepository(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage
) {

    suspend fun reportLocation(
        lat: Double,
        lng: Double,
        accuracyM: Float,
        source: String,
        capturedAt: Instant = Instant.now()
    ): Result<Unit> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.reportLocation(
                deviceId,
                LocationReportRequest(
                    lat = lat,
                    lng = lng,
                    accuracyM = accuracyM,
                    capturedAt = capturedAt.toString(),
                    source = source
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun sendSos(
        lat: Double,
        lng: Double,
        accuracyM: Float,
        note: String?,
        capturedAt: Instant = Instant.now()
    ): Result<Unit> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.sendSos(
                deviceId,
                SosRequest(
                    lat = lat,
                    lng = lng,
                    accuracyM = accuracyM,
                    capturedAt = capturedAt.toString(),
                    note = note
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
