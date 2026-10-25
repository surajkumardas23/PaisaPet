package com.example.paisapet.debugtools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.paisapet.AppContainer

/** Debug build: the Simulate SMS tool (FR-19). The release build has an empty stub with the same names. */
object DebugTools {
    const val isAvailable = true

    /** The Settings entry and page title. */
    const val TITLE = "Simulate SMS"

    @Composable
    fun Screen(container: AppContainer, modifier: Modifier = Modifier) = SimulateSmsScreen(container, modifier)
}
