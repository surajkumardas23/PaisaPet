package com.example.paisapet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.paisapet.ui.AppRoot
import com.example.paisapet.ui.theme.PaisaPetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only for now, so keep dark system-bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(LIGHT_SCRIM, DARK_SCRIM),
        )
        val container = (application as PaisaPetApp).container
        setContent {
            PaisaPetTheme {
                AppRoot(container)
            }
        }
    }

    private companion object {
        // The activity library's defaults: the scrim behind 3-button navigation, and the darker one used
        // where the system cannot draw dark navigation icons.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
