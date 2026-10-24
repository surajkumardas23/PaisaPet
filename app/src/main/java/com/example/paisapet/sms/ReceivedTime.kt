package com.example.paisapet.sms

private const val MAX_CLOCK_AHEAD_MS = 5 * 60 * 1000L

/** The SMS part's own timestamp, unless it is missing (<= 0) or more than 5 minutes in the future: then [nowMillis]. */
fun receivedAtMillis(partTimestampMillis: Long, nowMillis: Long): Long =
    if (partTimestampMillis <= 0 || partTimestampMillis > nowMillis + MAX_CLOCK_AHEAD_MS) nowMillis else partTimestampMillis
