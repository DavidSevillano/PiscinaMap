package com.burixer85.piscinamap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Abyssal dark — deep navy + cyan aquatic premium
private val AbyssalDarkScheme = darkColorScheme(
    primary             = AbyssalCyan,
    onPrimary           = AbyssalBg,
    primaryContainer    = AbyssalCyanSoft,
    onPrimaryContainer  = AbyssalCyan,
    secondary           = AbyssalTextDim,
    onSecondary         = AbyssalBg,
    secondaryContainer  = AbyssalSurface,
    onSecondaryContainer = AbyssalTextDim,
    tertiary            = AbyssalGreen,
    onTertiary          = AbyssalBg,
    tertiaryContainer   = Color(0xFF0A2E1A),
    onTertiaryContainer = AbyssalGreen,
    background          = AbyssalBg,
    onBackground        = AbyssalText,
    surface             = AbyssalSurface,
    onSurface           = AbyssalText,
    surfaceVariant      = AbyssalSurfaceHi,
    onSurfaceVariant    = AbyssalTextDim,
    outline             = AbyssalBorder,
    outlineVariant      = AbyssalBorderHi,
    error               = AbyssalRed,
    onError             = AbyssalText,
    errorContainer      = Color(0xFF3A1008),
    onErrorContainer    = AbyssalRed,
    inverseSurface      = AbyssalText,
    inverseOnSurface    = AbyssalBg,
    inversePrimary      = AbyssalCyanDeep,
)

// Daylight — white + deep cyan (light variant of Abyssal)
private val DaylightLightScheme = lightColorScheme(
    primary             = DaylightCyan,
    onPrimary           = DaylightBg,
    primaryContainer    = Color(0xFFCCEEF0),
    onPrimaryContainer  = DaylightCyan,
    secondary           = DaylightTextDim,
    onSecondary         = DaylightBg,
    secondaryContainer  = Color(0xFFE0EEF6),
    onSecondaryContainer = DaylightTextDim,
    tertiary            = DaylightGreen,
    onTertiary          = DaylightBg,
    tertiaryContainer   = Color(0xFFCCEEDE),
    onTertiaryContainer = DaylightGreen,
    background          = DaylightBg,
    onBackground        = DaylightText,
    surface             = DaylightSurface,
    onSurface           = DaylightText,
    surfaceVariant      = DaylightBg2,
    onSurfaceVariant    = DaylightTextDim,
    outline             = DaylightBorder,
    outlineVariant      = Color(0xFFD5E5EE),
    error               = DaylightRed,
    onError             = DaylightBg,
    errorContainer      = Color(0xFFFFE0DA),
    onErrorContainer    = DaylightRed,
    inverseSurface      = AbyssalBg,
    inverseOnSurface    = AbyssalText,
    inversePrimary      = AbyssalCyan,
)

@Composable
fun PiscinaMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AbyssalDarkScheme else DaylightLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
