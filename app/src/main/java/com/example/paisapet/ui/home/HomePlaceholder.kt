package com.example.paisapet.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.paisapet.AppContainer
import com.example.paisapet.debugtools.DebugTools

/** Placeholder until Phase 5 builds Home: Simulate SMS in debug builds, a plain title in release builds. */
@Composable
fun HomePlaceholder(container: AppContainer) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (DebugTools.isAvailable) {
            DebugTools.Screen(container, Modifier.padding(innerPadding))
        } else {
            Text("Paisa Pet", modifier = Modifier.padding(innerPadding))
        }
    }
}
