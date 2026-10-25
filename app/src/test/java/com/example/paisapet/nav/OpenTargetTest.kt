package com.example.paisapet.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenTargetTest {

    @Test fun homeWord() {
        assertEquals(OpenTarget.Home, parseOpenTarget(NO_OPEN_ID, "home"))
    }

    @Test fun addWord() {
        assertEquals(OpenTarget.AddExpense, parseOpenTarget(NO_OPEN_ID, "add"))
    }

    @Test fun paymentId() {
        assertEquals(OpenTarget.Payment(42), parseOpenTarget(42, null))
        assertEquals(OpenTarget.Payment(Long.MAX_VALUE), parseOpenTarget(Long.MAX_VALUE, null))
    }

    @Test fun validIdWinsOverAWord() {
        assertEquals(OpenTarget.Payment(7), parseOpenTarget(7, "home"))
        assertEquals(OpenTarget.Payment(7), parseOpenTarget(7, "add"))
        assertEquals(OpenTarget.Payment(7), parseOpenTarget(7, "nonsense"))
    }

    @Test fun invalidIdCountsAsAbsent() {
        assertEquals(OpenTarget.Home, parseOpenTarget(0, "home"))
        assertEquals(OpenTarget.AddExpense, parseOpenTarget(-1, "add"))
    }

    @Test fun nothingUsableOpensNothing() {
        assertNull(parseOpenTarget(NO_OPEN_ID, null))
        assertNull(parseOpenTarget(NO_OPEN_ID, ""))
        assertNull(parseOpenTarget(NO_OPEN_ID, " "))
        assertNull(parseOpenTarget(NO_OPEN_ID, "settings"))
        assertNull(parseOpenTarget(NO_OPEN_ID, "home "))
        assertNull(parseOpenTarget(NO_OPEN_ID, "\u0000add"))
    }

    /** Exact match only: our own intents always send lower case. */
    @Test fun capitalsAreUnknown() {
        assertNull(parseOpenTarget(NO_OPEN_ID, "ADD"))
        assertNull(parseOpenTarget(NO_OPEN_ID, "Home"))
    }

    @Test fun badIdsAlone() {
        assertNull(parseOpenTarget(0, null))
        assertNull(parseOpenTarget(-1, null))
        assertNull(parseOpenTarget(Long.MIN_VALUE, null))
        assertNull(parseOpenTarget(Long.MIN_VALUE, "garbage"))
    }

    @Test fun wordsAreTheExtraValues() {
        assertEquals("home", TARGET_HOME)
        assertEquals("add", TARGET_ADD)
    }

    @Test fun sameTargetTappedTwiceIsTwoRequests() {
        assertNotEquals(OpenRequest(OpenTarget.Home, 1), OpenRequest(OpenTarget.Home, 2))
        assertNotEquals(OpenRequest(OpenTarget.AddExpense, 3), OpenRequest(OpenTarget.AddExpense, 4))
    }
}
