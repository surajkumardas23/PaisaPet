package com.example.paisapet.nav

/**
 * "Open payment [id] in the Edit sheet", from a notification body tap. [seq] grows with every tap,
 * so tapping the same notification twice is two requests.
 */
data class OpenRequest(val id: Long, val seq: Int)

/** The default MainActivity passes to getLongExtra: no payment to open. */
const val NO_OPEN_ID = -1L

/** A valid transaction id from the intent extra, or null for a missing, zero or negative value. */
fun parseOpenId(raw: Long): Long? = raw.takeIf { it > 0 }

/**
 * Whether MainActivity.onCreate should read the extra: not when the activity is being restored
 * (the request was already applied and saved in NavState) and not when reopened from Recents
 * (Android replays the old launch intent).
 */
fun readsOpenExtra(restored: Boolean, launchedFromHistory: Boolean): Boolean = !restored && !launchedFromHistory
