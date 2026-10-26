package com.example.paisapet.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WideLayoutTest {

    @Test fun fullSizeShowsEverything() {
        val fit = wideFit(320f, 160f, 12f)
        assertTrue(fit.showBuddy)
        assertTrue(fit.showBudgetLine)
        assertTrue(fit.showLastPayment)
        // 320 - 24 padding - 76 Buddy column - 12 gap
        assertEquals(208f, fit.barTrackDp, 0f)
    }

    @Test fun buddyNeedsWidth() {
        assertTrue(wideFit(WIDE_BUDDY_MIN_WIDTH_DP, 160f, 12f).showBuddy)
        assertFalse(wideFit(WIDE_BUDDY_MIN_WIDTH_DP - 0.01f, 160f, 12f).showBuddy)
    }

    @Test fun withoutBuddyTheBarTakesTheWholeWidth() {
        assertEquals(176f, wideFit(200f, 160f, 12f).barTrackDp, 0f)
    }

    @Test fun minimumSizeDropsOnlyTheBudgetLine() {
        val fit = wideFit(250f, 110f, 12f)
        assertTrue(fit.showBuddy)
        assertTrue(fit.showLastPayment)
        assertFalse(fit.showBudgetLine)
    }

    @Test fun heightBoundaries() {
        assertTrue(wideFit(300f, WIDE_BUDGET_LINE_MIN_HEIGHT_DP, 12f).showBudgetLine)
        assertFalse(wideFit(300f, WIDE_BUDGET_LINE_MIN_HEIGHT_DP - 0.01f, 12f).showBudgetLine)
        assertTrue(wideFit(300f, WIDE_LAST_PAYMENT_MIN_HEIGHT_DP, 12f).showLastPayment)
        assertFalse(wideFit(300f, WIDE_LAST_PAYMENT_MIN_HEIGHT_DP - 0.01f, 12f).showLastPayment)
    }

    @Test fun tinyWidgetNeverHasANegativeBar() {
        assertEquals(0f, wideFit(10f, 40f, 12f).barTrackDp, 0f)
        assertEquals(0f, wideFit(0f, 0f, 12f).barTrackDp, 0f)
    }
}
