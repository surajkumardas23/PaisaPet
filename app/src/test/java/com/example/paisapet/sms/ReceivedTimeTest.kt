package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceivedTimeTest {

    private val now = 1_780_000_000_000L
    private val minute = 60_000L

    @Test fun normalTimestampKept() = assertEquals(now - 3 * minute, receivedAtMillis(now - 3 * minute, now))

    @Test fun zeroUsesNow() = assertEquals(now, receivedAtMillis(0, now))

    @Test fun negativeUsesNow() = assertEquals(now, receivedAtMillis(-1, now))

    @Test fun fourMinutesAheadKept() = assertEquals(now + 4 * minute, receivedAtMillis(now + 4 * minute, now))

    @Test fun exactlyFiveMinutesAheadKept() = assertEquals(now + 5 * minute, receivedAtMillis(now + 5 * minute, now))

    @Test fun sixMinutesAheadUsesNow() = assertEquals(now, receivedAtMillis(now + 6 * minute, now))
}
