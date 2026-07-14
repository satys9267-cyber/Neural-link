package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = ElectricBlue,
    tertiary = AmberAccent,
    background = SlateDarkBackground,
    surface = ObsidianSurface,
    surfaceVariant = TintedSlateSurface,
    onPrimary = SlateDarkBackground,
    onSecondary = SlateDarkBackground,
    onTertiary = SlateDarkBackground,
    onBackground = TextSilver,
    onSurface = TextSilver,
    onSurfaceVariant = TextSilver,
    error = CyberRed,
    onError = TextSilver
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}

