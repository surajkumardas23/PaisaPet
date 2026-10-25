package com.example.paisapet.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRequestTest {

    @Test fun positiveIdsAreKept() {
        assertEquals(1L, parseOpenId(1))
        assertEquals(42L, parseOpenId(42))
        assertEquals(Long.MAX_VALUE, parseOpenId(Long.MAX_VALUE))
    }

    @Test fun zeroAndNegativeAreRejected() {
        assertNull(parseOpenId(0))
        assertNull(parseOpenId(-1))
        assertNull(parseOpenId(Long.MIN_VALUE))
    }

    @Test fun theMissingExtraDefaultIsRejected() {
        assertNull(parseOpenId(NO_OPEN_ID))
    }

    @Test fun extraIsReadOnlyOnAFreshLaunch() {
        assertTrue(readsOpenExtra(restored = false, launchedFromHistory = false))
        assertFalse(readsOpenExtra(restored = true, launchedFromHistory = false))
        assertFalse(readsOpenExtra(restored = false, launchedFromHistory = true))
        assertFalse(readsOpenExtra(restored = true, launchedFromHistory = true))
    }

    @Test fun sameIdTappedTwiceIsTwoRequests() {
        assertNotEquals(OpenRequest(OpenTarget.Payment(7), 1), OpenRequest(OpenTarget.Payment(7), 2))
    }
}
