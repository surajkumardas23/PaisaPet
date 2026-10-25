package com.example.paisapet.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartGateTest {

    @Test fun stillReading_isLoading() = assertEquals(StartScreen.LOADING, startScreen(null))

    @Test fun notComplete_isOnboarding() = assertEquals(StartScreen.ONBOARDING, startScreen(false))

    @Test fun complete_isHome() = assertEquals(StartScreen.HOME, startScreen(true))

    @Test fun backFromWelcome_leavesApp() = assertNull(OnboardingStep.WELCOME.back())

    @Test fun backFromBudget_isWelcome() = assertEquals(OnboardingStep.WELCOME, OnboardingStep.BUDGET.back())

    @Test fun backFromPermissions_isBudget() = assertEquals(OnboardingStep.BUDGET, OnboardingStep.PERMISSIONS.back())
}
