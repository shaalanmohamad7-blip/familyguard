package com.familyguard.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GeofenceDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float
)

@Serializable
data class GeofencesResponse(
    val geofences: List<GeofenceDto> = emptyList()
)

@Serializable
data class GeofenceEventRequest(
    val geofenceId: String,
    val transition: String,
    val occurredAt: String,
    val lat: Double,
    val lng: Double
)
