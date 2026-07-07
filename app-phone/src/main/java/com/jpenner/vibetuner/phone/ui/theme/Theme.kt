package com.jpenner.vibetuner.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VibeTunerColorScheme = darkColorScheme(
    primary = PhoneColors.Accent,
    onPrimary = PhoneColors.AccentInk,
    secondary = PhoneColors.Accent2,
    background = PhoneColors.Bg,
    onBackground = PhoneColors.Txt,
    surface = PhoneColors.Surface,
    onSurface = PhoneColors.Txt,
    surfaceVariant = PhoneColors.Raised,
    onSurfaceVariant = PhoneColors.Txt2,
    outline = PhoneColors.Line,
    error = PhoneColors.Live,
)

/**
 * VibeTuner is a single dark-authored brand (no light theme in the design kit),
 * so this ignores [isSystemInDarkTheme] and always applies the Aerial palette.
 */
@Composable
fun VibeTunerPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibeTunerColorScheme,
        typography = PhoneTypography,
        content = content,
    )
}
