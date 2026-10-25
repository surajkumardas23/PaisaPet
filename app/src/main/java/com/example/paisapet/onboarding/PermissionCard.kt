package com.example.paisapet.onboarding

/** What an S3 permission card shows. */
enum class PermissionCard {
    /** Notifications below Android 13: no runtime permission exists. */
    NOT_NEEDED,
    ALLOWED,

    /** Not asked yet, as far as Android tells us: Allow. */
    ASK,

    /** Denied, but Android will still show the dialog: "Not allowed" + Allow. */
    DENIED_CAN_ASK,

    /** Denied and Android will not show the dialog (permanent, or a restricted setting): Open Settings. */
    BLOCKED,
}

/** Android 13 (API 33) introduced POST_NOTIFICATIONS. */
private const val NOTIFICATIONS_PERMISSION_SDK = 33

/** Android 13 (API 33) introduced restricted settings for sideloaded apps. */
private const val RESTRICTED_SETTINGS_SDK = 33

/**
 * [required]: false only for notifications below Android 13. [showRationale]: shouldShowRequestPermissionRationale
 * right now. [deniedThisVisit]: a request from this screen came back denied.
 */
fun permissionCard(
    required: Boolean,
    granted: Boolean,
    showRationale: Boolean,
    deniedThisVisit: Boolean,
): PermissionCard = when {
    !required -> PermissionCard.NOT_NEEDED
    granted -> PermissionCard.ALLOWED
    showRationale -> PermissionCard.DENIED_CAN_ASK
    // Denied with no rationale: Android will not show the dialog again (or never showed it).
    deniedThisVisit -> PermissionCard.BLOCKED
    else -> PermissionCard.ASK
}

fun notificationsRequired(sdkInt: Int): Boolean = sdkInt >= NOTIFICATIONS_PERMISSION_SDK

/** The "SMS Permission Denied" card shows only once Android has told us SMS was denied. */
fun showSmsDeniedCard(sms: PermissionCard): Boolean =
    sms == PermissionCard.DENIED_CAN_ASK || sms == PermissionCard.BLOCKED

/** The "Allow restricted settings" hint: a blocked SMS request on Android 13+. */
fun showRestrictedHint(sms: PermissionCard, sdkInt: Int): Boolean =
    sms == PermissionCard.BLOCKED && sdkInt >= RESTRICTED_SETTINGS_SDK
