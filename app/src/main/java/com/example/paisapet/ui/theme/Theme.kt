package com.example.paisapet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light only for now; the dark theme is P1. No dynamic colour: it would replace the royal blue with wallpaper colours.
private val LightColorScheme = lightColorScheme(
    primary = PaisaBlue,
    onPrimary = Color.White,
    background = AppBackground,
    onBackground = InkText,
    surface = CardWhite,
    onSurface = InkText,
    surfaceVariant = HelperPill,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = CardWhite,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
)

@Composable
fun PaisaPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
