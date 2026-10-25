package com.example.paisapet.nav

/** Where a tap from outside the app (notification, widget, alert) should land. */
sealed interface OpenTarget {
    /** The payment's Edit sheet (notification body tap). */
    data class Payment(val id: Long) : OpenTarget
    /** Home tab, current month, nothing open. */
    data object Home : OpenTarget
    /** Home plus the Add expense sheet. */
    data object AddExpense : OpenTarget
}

/** "Open [target]". [seq] grows with every tap, so tapping the same thing twice is two requests. */
data class OpenRequest(val target: OpenTarget, val seq: Int)

/** Intent extra naming a non-payment target: [TARGET_HOME] or [TARGET_ADD]. */
const val EXTRA_OPEN_TARGET = "com.example.paisapet.extra.OPEN_TARGET"
const val TARGET_HOME = "home"
const val TARGET_ADD = "add"

/** The default MainActivity passes to getLongExtra: no payment to open. */
const val NO_OPEN_ID = -1L

/** A valid transaction id from the intent extra, or null for a missing, zero or negative value. */
fun parseOpenId(raw: Long): Long? = raw.takeIf { it > 0 }

/**
 * The target from the two intent extras. A valid [rawId] wins over [targetWord]; an invalid id counts as absent.
 * [targetWord] must match exactly ("home", "add"): our own intents always send lower case, so "ADD" is null.
 * Null means nothing is opened.
 */
fun parseOpenTarget(rawId: Long, targetWord: String?): OpenTarget? {
    parseOpenId(rawId)?.let { return OpenTarget.Payment(it) }
    return when (targetWord) {
        TARGET_HOME -> OpenTarget.Home
        TARGET_ADD -> OpenTarget.AddExpense
        else -> null
    }
}

/**
 * Whether MainActivity.onCreate should read the extras: not when the activity is being restored
 * (the request was already applied and saved in NavState) and not when reopened from Recents
 * (Android replays the old launch intent).
 */
fun readsOpenExtra(restored: Boolean, launchedFromHistory: Boolean): Boolean = !restored && !launchedFromHistory
