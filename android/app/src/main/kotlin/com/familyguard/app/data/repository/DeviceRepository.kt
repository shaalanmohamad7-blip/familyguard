package com.familyguard.app.data.repository

import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.data.remote.ApiService
import com.familyguard.app.data.remote.dto.FcmTokenRequest
import com.familyguard.app.data.remote.dto.PermissionSnapshotDto
import com.familyguard.app.data.remote.dto.StatusRequest
import com.familyguard.app.domain.model.Guardian
import com.familyguard.app.domain.model.PermissionSnapshot

class DeviceRepository(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val appVersion: String
) {

    suspend fun getFamilyInfo(): Result<List<Guardian>> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.getFamilyInfo(deviceId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.guardians.map { Guardian(it.id, it.displayName, it.relationship) })
            } else {
                Result.failure(IllegalStateException("Request failed with ${response.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun sendHeartbeat(batteryPercent: Int, permissions: PermissionSnapshot): Result<Unit> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.reportStatus(
                deviceId,
                StatusRequest(
                    batteryPercent = batteryPercent,
                    permissions = permissions.toDto(),
                    appVersion = appVersion
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun registerFcmToken(token: String): Result<Unit> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        return try {
            val response = apiService.registerFcmToken(deviceId, FcmTokenRequest(token))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Unpairs this device. Local state is cleared regardless of whether the network call
     * succeeds, because the child's ability to stop sharing must never depend on a server
     * round-trip succeeding.
     */
    suspend fun unpair(): Result<Unit> {
        val deviceId = secureStorage.deviceId()
        val networkResult = if (deviceId != null) {
            try {
                val response = apiService.unpairDevice(deviceId)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
            } catch (t: Throwable) {
                Result.failure(t)
            }
        } else {
            Result.success(Unit)
        }
        secureStorage.clearPairing()
        return networkResult
    }

    private fun PermissionSnapshot.toDto() = PermissionSnapshotDto(
        foregroundLocationGranted = foregroundLocationGranted,
        backgroundLocationGranted = backgroundLocationGranted,
        notificationsGranted = notificationsGranted,
        usageAccessGranted = usageAccessGranted
    )
}
