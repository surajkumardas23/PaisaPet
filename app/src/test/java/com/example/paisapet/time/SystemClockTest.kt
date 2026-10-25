package com.example.paisapet.time

import com.example.paisapet.budget.monthKeyOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

class SystemClockTest {

    /** Runs [block] with the JVM default zone set to [zoneId], then restores the original. */
    private fun <T> withDefaultZone(zoneId: String, block: () -> T): T {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
            return block()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun instantMovesForward() {
        val clock = SystemClock()
        val first = clock.instant()
        Thread.sleep(5)
        val second = clock.instant()
        assertTrue("$second should be after $first", second.isAfter(first))
        assertTrue(clock.millis() >= second.toEpochMilli())
    }

    @Test fun zoneEqualsSystemDefaultWhenCalled() {
        assertEquals(ZoneId.systemDefault(), SystemClock().zone)
    }

    @Test fun withZoneReturnsAClockInThatZone() {
        val ny = ZoneId.of("America/New_York")
        val clock = SystemClock().withZone(ny)
        assertEquals(ny, clock.zone)
    }

    @Test fun zoneFollowsADefaultChangeWithoutRecreatingTheClock() {
        val clock = SystemClock()
        val fixed = Instant.parse("2026-10-31T20:00:00Z")
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            assertEquals(ZoneId.of("Asia/Kolkata"), clock.zone)
            // 20:00 UTC on 31 Oct is 01:30 on 1 Nov in India.
            assertEquals("2026-11", monthKeyOf(fixed, clock.zone))

            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals(ZoneId.of("America/New_York"), clock.zone)
            // ...and 16:00 on 31 Oct in New York.
            assertEquals("2026-10", monthKeyOf(fixed, clock.zone))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun liveMonthKeyUsesTheZoneAtTheMomentOfReading() {
        val clock = SystemClock()
        val inIndia = withDefaultZone("Asia/Kolkata") { monthKeyOf(clock.instant(), clock.zone) }
        val inIndiaExpected = withDefaultZone("Asia/Kolkata") { monthKeyOf(Instant.now(), ZoneId.systemDefault()) }
        assertEquals(inIndiaExpected, inIndia)
    }

    @Test fun defaultZoneIsRestoredAfterTheChange() {
        val before = TimeZone.getDefault()
        withDefaultZone("America/New_York") { }
        assertEquals(before, TimeZone.getDefault())
    }
}
