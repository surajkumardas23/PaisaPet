package com.example.paisapet.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.paisapet.time.millisUntilNextMidnight
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.LocalDate

/**
 * Today's date in the phone's current time zone (the clock reads it at every call).
 * Recomputed when the screen resumes and just after each local midnight, so the month switches by itself (FR-16).
 */
@Composable
fun rememberToday(clock: Clock): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now(clock)) }
    LifecycleResumeEffect(clock) {
        today = LocalDate.now(clock)
        onPauseOrDispose { }
    }
    LaunchedEffect(clock) {
        while (true) {
            delay(millisUntilNextMidnight(clock.instant(), clock.zone))
            today = LocalDate.now(clock)
        }
    }
    return today
}
