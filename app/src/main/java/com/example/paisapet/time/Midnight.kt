package com.example.paisapet.time

import java.time.Instant
import java.time.ZoneId

/** Milliseconds until the next local midnight (the start of tomorrow), at least 1. Days of 23 or 25 hours are handled. */
fun millisUntilNextMidnight(now: Instant, zone: ZoneId): Long {
    val tomorrow = now.atZone(zone).toLocalDate().plusDays(1)
    // atStartOfDay picks the first valid time when a DST gap skips midnight.
    val next = tomorrow.atStartOfDay(zone).toInstant()
    return maxOf(1L, next.toEpochMilli() - now.toEpochMilli())
}
