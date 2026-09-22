package com.familyguard.app.util

/** Central place for fixed, non-secret configuration values. */
object Constants {

    const val PLATFORM = "ANDROID"

    // Notification channels
    const val CHANNEL_LOCATION_SHARING = "location_sharing"
    const val CHANNEL_ALERTS = "familyguard_alerts"

    // Notification ids
    const val NOTIF_ID_LOCATION_SHARING = 1001
    const val NOTIF_ID_SOS = 1002
    const val NOTIF_ID_UNPAIRED = 1003

    // WorkManager unique work names
    const val WORK_HEARTBEAT = "familyguard_heartbeat_worker"
    const val WORK_USAGE_REPORT = "familyguard_usage_report_worker"
    const val WORK_BOOT_RESYNC = "familyguard_boot_resync_worker"

    // Intervals
    const val HEARTBEAT_INTERVAL_MINUTES = 30L
    const val USAGE_REPORT_INTERVAL_HOURS = 24L
    const val LOCATION_UPDATE_INTERVAL_MS = 15L * 60L * 1000L // 15 minutes
    const val LOCATION_FASTEST_INTERVAL_MS = 5L * 60L * 1000L // 5 minutes

    // Location sources sent to the backend (kept as plain strings — the backend's contract,
    // not an enum owned by this app).
    const val SOURCE_BACKGROUND = "BACKGROUND"
    const val SOURCE_CHECK_IN = "CHECK_IN"
    const val SOURCE_PUSH_REQUEST = "PUSH_REQUEST"

    // FCM data-message contract
    const val FCM_KEY_TYPE = "type"
    const val FCM_TYPE_REQUEST_LOCATION = "request-location"

    // Geofence broadcast
    const val ACTION_GEOFENCE_EVENT = "com.familyguard.app.action.GEOFENCE_EVENT"
}
