package com.example.paisapet.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paisapet.AppContainer
import com.example.paisapet.onboarding.OnboardingStep
import com.example.paisapet.onboarding.back
import kotlinx.coroutines.launch

/** S1 → S2 → S3. Finishing sets onboardingComplete; AppRoot then switches to Home by itself. */
@Composable
fun OnboardingFlow(container: AppContainer) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    val scope = rememberCoroutineScope()

    // On WELCOME there is no previous step, so the handler is off and the system closes the app.
    val previous = step.back()
    BackHandler(enabled = previous != null) { previous?.let { step = it } }

    when (step) {
        OnboardingStep.WELCOME -> WelcomeScreen(onGetStarted = { step = OnboardingStep.BUDGET })
        OnboardingStep.BUDGET -> StepPlaceholder(
            title = "Set budget",
            nextLabel = "Continue",
            onNext = { step = OnboardingStep.PERMISSIONS },
            onBack = { step = OnboardingStep.WELCOME },
        )
        OnboardingStep.PERMISSIONS -> StepPlaceholder(
            title = "Permissions",
            nextLabel = "Go to Home",
            onNext = { scope.launch { container.settings.setOnboardingComplete(true) } },
            onBack = { step = OnboardingStep.BUDGET },
        )
    }
}

/** Placeholder, replaced in a later step (S2 and S3). */
@Composable
private fun StepPlaceholder(title: String, nextLabel: String, onNext: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("$title (placeholder)", style = MaterialTheme.typography.headlineSmall)
        PrimaryPillButton(text = nextLabel, onClick = onNext, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}
