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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paisapet.AppContainer
import com.example.paisapet.onboarding.BudgetInput
import com.example.paisapet.onboarding.OnboardingStep
import com.example.paisapet.onboarding.back
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/** S1 → S2 → S3. Finishing sets onboardingComplete; AppRoot then switches to Home by itself. */
@Composable
fun OnboardingFlow(container: AppContainer) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    // Kept here, not in S2, so Back and forward keep what was typed.
    var budgetText by rememberSaveable { mutableStateOf("") }
    var budgetEdited by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // On WELCOME there is no previous step, so the handler is off and the system closes the app.
    val previous = step.back()
    BackHandler(enabled = previous != null) { previous?.let { step = it } }

    when (step) {
        OnboardingStep.WELCOME -> WelcomeScreen(onGetStarted = { step = OnboardingStep.BUDGET })
        OnboardingStep.BUDGET -> {
            // Pre-fill once: the saved budget after a restart, else ₹5,000. Never overwrites the user's text.
            LaunchedEffect(Unit) {
                if (budgetEdited || budgetText.isNotEmpty()) return@LaunchedEffect
                val saved = try {
                    container.settings.defaultBudgetPaise.first()
                } catch (e: IOException) {
                    null
                }
                if (!budgetEdited && budgetText.isEmpty()) budgetText = BudgetInput.prefillText(saved)
            }
            SetBudgetScreen(
                text = budgetText,
                onTextChange = {
                    budgetText = it
                    budgetEdited = true
                    saveFailed = false
                },
                saving = saving,
                saveFailed = saveFailed,
                onBack = { step = OnboardingStep.WELCOME },
                onContinue = { paise ->
                    if (!saving) {
                        saving = true
                        saveFailed = false
                        scope.launch {
                            val saved = try {
                                container.repository.setBudget(paise)
                                true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                false
                            }
                            saving = false
                            if (saved) step = OnboardingStep.PERMISSIONS else saveFailed = true
                        }
                    }
                },
            )
        }
        OnboardingStep.PERMISSIONS -> StepPlaceholder(
            title = "Permissions",
            nextLabel = "Go to Home",
            onNext = { scope.launch { container.settings.setOnboardingComplete(true) } },
            onBack = { step = OnboardingStep.BUDGET },
        )
    }
}

/** Placeholder, replaced in a later step (S3). */
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
