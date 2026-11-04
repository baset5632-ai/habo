package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = ReactorCyan,
    onPrimary = CyberObsidian,
    primaryContainer = Color(0xFF0A2E46),
    onPrimaryContainer = ReactorCyan,
    secondary = SolarGold,
    onSecondary = CyberObsidian,
    secondaryContainer = Color(0xFF3B2A08),
    onSecondaryContainer = SolarGold,
    tertiary = EmeraldPulse,
    onTertiary = CyberObsidian,
    background = CyberObsidian,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = CrimsonAlert,
    onError = Color.White,
    outline = CyberCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
