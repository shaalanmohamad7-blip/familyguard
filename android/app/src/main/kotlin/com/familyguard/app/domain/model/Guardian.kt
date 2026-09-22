package com.familyguard.app.domain.model

/** A guardian on the family account who can see this device's shared data. */
data class Guardian(
    val id: String,
    val displayName: String,
    val relationship: String?
)

/** A parent-defined circular geofence this device should watch for enter/exit transitions. */
data class GeofenceDefinition(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)

/** One app-usage summary record for a single day, reported only when Usage Access is granted. */
data class UsageRecord(
    val appPackage: String,
    val appLabel: String,
    val minutes: Long
)
