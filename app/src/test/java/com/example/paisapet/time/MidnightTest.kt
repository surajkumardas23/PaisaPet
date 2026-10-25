package com.example.paisapet.time

import com.example.paisapet.IST
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MidnightTest {

    private fun at(t: LocalDateTime, zone: ZoneId) = t.atZone(zone).toInstant()

    @Test fun normalDay() {
        val now = at(LocalDateTime.of(2026, 10, 25, 18, 0), IST)
        assertEquals(6 * 3_600_000L, millisUntilNextMidnight(now, IST))
    }

    @Test fun oneMillisecondBeforeMidnight() {
        val now = at(LocalDateTime.of(2026, 10, 31, 23, 59, 59, 999_000_000), IST)
        assertEquals(1L, millisUntilNextMidnight(now, IST))
    }

    @Test fun exactlyMidnightWaitsAFullDay() {
        val now = at(LocalDateTime.of(2026, 11, 1, 0, 0), IST)
        assertEquals(24 * 3_600_000L, millisUntilNextMidnight(now, IST))
    }

    @Test fun shortDstDay() {
        // New York springs forward on 8 Mar 2026: that day has 23 hours.
        val ny = ZoneId.of("America/New_York")
        val now = at(LocalDateTime.of(2026, 3, 8, 0, 0), ny)
        assertEquals(23 * 3_600_000L, millisUntilNextMidnight(now, ny))
    }

    @Test fun longDstDay() {
        // New York falls back on 1 Nov 2026: that day has 25 hours.
        val ny = ZoneId.of("America/New_York")
        val now = at(LocalDateTime.of(2026, 11, 1, 0, 0), ny)
        assertEquals(25 * 3_600_000L, millisUntilNextMidnight(now, ny))
    }
}
