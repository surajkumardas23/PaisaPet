package com.example.paisapet.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The app's clock. Unlike Clock.systemDefaultZone(), which fixes the zone when it is created,
 * this reads the phone's time zone every time [getZone] is called, so a zone change applies at once.
 */
class SystemClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
