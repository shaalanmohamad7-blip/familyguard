package com.familyguard.app.data.repository

import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.data.remote.ApiService
import com.familyguard.app.data.remote.dto.UsageRecordDto
import com.familyguard.app.data.remote.dto.UsageReportRequest
import com.familyguard.app.domain.model.UsageRecord

class UsageRepository(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage
) {

    suspend fun reportUsage(date: String, records: List<UsageRecord>): Result<Unit> {
        val deviceId = secureStorage.deviceId() ?: return Result.failure(IllegalStateException("Not paired"))
        if (records.isEmpty()) return Result.success(Unit)
        return try {
            val response = apiService.reportUsage(
                deviceId,
                UsageReportRequest(
                    date = date,
                    records = records.map { UsageRecordDto(it.appPackage, it.appLabel, it.minutes) }
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("Request failed with ${response.code()}"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
