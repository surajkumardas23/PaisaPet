package com.example.paisapet.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsModelTest {

    @Test fun budgetText() {
        assertEquals("₹5,000", budgetDisplayText(5_000_00))
        assertEquals("₹10,00,000", budgetDisplayText(10_00_000_00))
        assertEquals("₹100", budgetDisplayText(100_00))
        assertEquals("Not set", budgetDisplayText(null))
    }

    @Test fun versionTextWithFallback() {
        assertEquals("Version 1.0", versionText("1.0"))
        assertEquals("Version unknown", versionText(null))
        assertEquals("Version unknown", versionText(""))
        assertEquals("Version unknown", versionText("  "))
    }

    @Test fun smsStatus() {
        assertEquals("Granted", smsStatusText(true))
        assertEquals("Not granted", smsStatusText(false))
    }

    @Test fun notificationStatus() {
        assertEquals("Granted", notificationStatusText(required = true, granted = true))
        assertEquals("Not granted", notificationStatusText(required = true, granted = false))
        // Android 12 and below: no permission exists.
        assertEquals("Not needed", notificationStatusText(required = false, granted = false))
        assertEquals("Not needed", notificationStatusText(required = false, granted = true))
    }
}
