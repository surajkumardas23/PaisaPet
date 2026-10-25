package com.example.paisapet.ui.settings

import com.example.paisapet.format.formatRupees

// Pure: the texts Settings (S9) shows. No Compose here.

/** The current month's budget, or "Not set" when there is none. */
fun budgetDisplayText(budgetPaise: Long?): String = budgetPaise?.let(::formatRupees) ?: "Not set"

/** "Version 1.0", or "Version unknown" when the package could not be read. */
fun versionText(versionName: String?): String =
    if (versionName.isNullOrBlank()) "Version unknown" else "Version $versionName"

fun smsStatusText(granted: Boolean): String = if (granted) "Granted" else "Not granted"

/** [required] is false below Android 13, where no notification permission exists. */
fun notificationStatusText(required: Boolean, granted: Boolean): String = when {
    !required -> "Not needed"
    granted -> "Granted"
    else -> "Not granted"
}
