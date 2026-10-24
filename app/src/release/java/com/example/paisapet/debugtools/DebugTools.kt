package com.example.paisapet.debugtools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.paisapet.AppContainer

/** Release stub: Simulate SMS is absent from release builds (FR-19). */
object DebugTools {
    const val isAvailable = false

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun Screen(container: AppContainer, modifier: Modifier = Modifier) = Unit
}
