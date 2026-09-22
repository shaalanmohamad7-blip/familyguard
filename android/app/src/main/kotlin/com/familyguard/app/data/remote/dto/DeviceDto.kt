package com.familyguard.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GuardianDto(
    val id: String,
    val displayName: String,
    val relationship: String? = null
)

@Serializable
data class FamilyInfoResponse(
    val guardians: List<GuardianDto> = emptyList()
)

@Serializable
data class PermissionSnapshotDto(
    val foregroundLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val notificationsGranted: Boolean,
    val usageAccessGranted: Boolean
)

@Serializable
data class StatusRequest(
    val batteryPercent: Int,
    val permissions: PermissionSnapshotDto,
    val appVersion: String
)

@Serializable
data class FcmTokenRequest(
    val token: String
)

@Serializable
data class UnpairResponse(
    val success: Boolean = true
)
