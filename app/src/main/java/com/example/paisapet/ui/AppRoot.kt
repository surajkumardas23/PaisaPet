package com.example.paisapet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.paisapet.AppContainer
import com.example.paisapet.onboarding.StartScreen
import com.example.paisapet.onboarding.startScreen
import com.example.paisapet.ui.home.HomePlaceholder
import com.example.paisapet.ui.onboarding.OnboardingFlow
import kotlinx.coroutines.flow.map

/** Start-up gate: a blank background while onboardingComplete is read, then onboarding or Home. */
@Composable
fun AppRoot(container: AppContainer) {
    val onboardingComplete by remember {
        container.settings.onboardingComplete.map<Boolean, Boolean?> { it }
    }.collectAsState(initial = null)

    when (startScreen(onboardingComplete)) {
        StartScreen.LOADING -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        StartScreen.ONBOARDING -> OnboardingFlow(container)
        StartScreen.HOME -> HomePlaceholder(container)
    }
}
