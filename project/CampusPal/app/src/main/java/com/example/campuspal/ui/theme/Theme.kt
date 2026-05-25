package com.example.campuspal.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun sunsetColorScheme() = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    tertiary = SunsetTertiary,
    onTertiary = SunsetOnTertiary,
    background = SunsetBackground,
    surface = SunsetSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onBackground = SunsetOnBackground,
    onSurface = SunsetOnSurface,
    onSurfaceVariant = SunsetOnSurfaceVariant,
    outline = SunsetOutline,
    error = ErrorRed,
)

fun oceanColorScheme() = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    tertiary = OceanTertiary,
    onTertiary = OceanOnTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    surfaceVariant = OceanSurfaceVariant,
    onBackground = OceanOnBackground,
    onSurface = OceanOnSurface,
    onSurfaceVariant = OceanOnSurfaceVariant,
    outline = OceanOutline,
    error = ErrorRed,
)

fun cherryColorScheme() = lightColorScheme(
    primary = CherryPrimary,
    onPrimary = CherryOnPrimary,
    primaryContainer = CherryPrimaryContainer,
    onPrimaryContainer = CherryOnPrimaryContainer,
    secondary = CherrySecondary,
    onSecondary = CherryOnSecondary,
    secondaryContainer = CherrySecondaryContainer,
    tertiary = CherryTertiary,
    onTertiary = CherryOnTertiary,
    background = CherryBackground,
    surface = CherrySurface,
    surfaceVariant = CherrySurfaceVariant,
    onBackground = CherryOnBackground,
    onSurface = CherryOnSurface,
    onSurfaceVariant = CherryOnSurfaceVariant,
    outline = CherryOutline,
    error = ErrorRed,
)

fun forestColorScheme() = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    background = ForestBackground,
    surface = ForestSurface,
    surfaceVariant = ForestSurfaceVariant,
    onBackground = ForestOnBackground,
    onSurface = ForestOnSurface,
    onSurfaceVariant = ForestOnSurfaceVariant,
    outline = ForestOutline,
    error = ErrorRed,
)

fun midnightColorScheme() = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    onSecondary = MidnightOnSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = MidnightTertiary,
    onTertiary = MidnightOnTertiary,
    background = MidnightBackground,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = MidnightOnBackground,
    onSurface = MidnightOnSurface,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    outline = MidnightOutline,
    error = Color(0xFFE0706A),
)

@Composable
fun CampusPalTheme(
    isDarkTheme: Boolean = false,
    colorSchemeType: ColorSchemeType = ColorSchemeType.SUNSET,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        isDarkTheme -> midnightColorScheme()
        colorSchemeType == ColorSchemeType.OCEAN -> oceanColorScheme()
        colorSchemeType == ColorSchemeType.CHERRY -> cherryColorScheme()
        colorSchemeType == ColorSchemeType.FOREST -> forestColorScheme()
        else -> sunsetColorScheme()
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
