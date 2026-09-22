package com.familyguard.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationReportRequest(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float,
    val capturedAt: String,
    val source: String
)

@Serializable
data class SosRequest(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float,
    val capturedAt: String,
    val note: String? = null
)

@Serializable
data class GenericSuccessResponse(
    val success: Boolean = true
)
