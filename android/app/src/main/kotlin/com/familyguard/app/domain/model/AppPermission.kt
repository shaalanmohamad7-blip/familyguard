package com.familyguard.app.domain.model

/**
 * Every permission FamilyGuard can ask for. Each one is independently optional — a permission
 * the child skips or denies simply means the feature that depends on it is honestly reported
 * as "off" everywhere in the UI. Nothing is ever silently assumed granted.
 */
enum class AppPermission {
    FOREGROUND_LOCATION,
    BACKGROUND_LOCATION,
    NOTIFICATIONS,
    USAGE_ACCESS
}

/** Current, always-live state of one permission — never cached longer than a single read. */
data class PermissionSnapshot(
    val foregroundLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val notificationsGranted: Boolean,
    val usageAccessGranted: Boolean
) {
    /** Continuous background sharing needs both foreground and background location. */
    val canShareLocationContinuously: Boolean
        get() = foregroundLocationGranted && backgroundLocationGranted

    /** One-off fixes (SOS, check-in) only need foreground/precise location. */
    val canGetOneTimeLocation: Boolean
        get() = foregroundLocationGranted
}
