package com.example.paisapet.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoTrackingBannerTest {

    @Test fun showsAfterOnboardingWhenSmsIsOff() {
        assertTrue(showAutoTrackingBanner(onboardingComplete = true, smsGranted = false))
    }

    @Test fun hiddenOnceSmsIsGranted() {
        assertFalse(showAutoTrackingBanner(onboardingComplete = true, smsGranted = true))
    }

    @Test fun neverDuringOnboarding() {
        assertFalse(showAutoTrackingBanner(onboardingComplete = false, smsGranted = false))
        assertFalse(showAutoTrackingBanner(onboardingComplete = false, smsGranted = true))
    }
}
