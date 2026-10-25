package com.example.paisapet.onboarding

/** What the app shows at start-up. */
enum class StartScreen { LOADING, ONBOARDING, HOME }

/** [onboardingComplete] is null while it is still being read. */
fun startScreen(onboardingComplete: Boolean?): StartScreen = when (onboardingComplete) {
    null -> StartScreen.LOADING
    false -> StartScreen.ONBOARDING
    true -> StartScreen.HOME
}

/** S1 → S2 → S3. */
enum class OnboardingStep { WELCOME, BUDGET, PERMISSIONS }

/** Where Back goes; null on WELCOME, so the system closes the app. */
fun OnboardingStep.back(): OnboardingStep? = when (this) {
    OnboardingStep.WELCOME -> null
    OnboardingStep.BUDGET -> OnboardingStep.WELCOME
    OnboardingStep.PERMISSIONS -> OnboardingStep.BUDGET
}
