package com.example.paisapet.widget

import com.example.paisapet.budget.BuddyState
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPartsTest {

    @Test fun barFillFollowsThePercent() {
        assertEquals(0f, barFillDp(200f, 0), 0f)
        assertEquals(128f, barFillDp(200f, 64), 0.001f)
        assertEquals(200f, barFillDp(200f, 100), 0f)
    }

    @Test fun barFillIsClamped() {
        assertEquals(200f, barFillDp(200f, 300), 0f)
        assertEquals(0f, barFillDp(200f, -5), 0f)
    }

    @Test fun noTrackNoFill() {
        assertEquals(0f, barFillDp(0f, 50), 0f)
        assertEquals(0f, barFillDp(-24f, 50), 0f)
    }

    @Test fun messages() {
        assertEquals("Set up Paisa Pet", messageFor(WidgetModel.SetUp))
        assertEquals("Set your budget", messageFor(WidgetModel.NoBudget))
        assertEquals("Open Paisa Pet", messageFor(null))
    }

    @Test fun budgetedHasNoMessageButFallsBackSafely() {
        val m = WidgetModel.Budgeted(BuddyState.FINE, "₹1", false, "left of ₹2", "Budget ₹2 for Oct", "50% used", 50, null)
        assertEquals(WIDGET_ERROR_TEXT, messageFor(m))
    }
}
