package com.familyguard.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsageRecordDto(
    val appPackage: String,
    val appLabel: String,
    val minutes: Long
)

@Serializable
data class UsageReportRequest(
    val date: String,
    val records: List<UsageRecordDto>
)
