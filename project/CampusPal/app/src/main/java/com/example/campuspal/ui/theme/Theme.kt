package com.example.campuspal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

// 全局圆角系统
val LargeRounding = 24.dp
val MediumRounding = 16.dp
val SmallRounding = 10.dp
val TinyRounding = 6.dp

val CampusPalShapes = Shapes(
    extraLarge = RoundedCornerShape(LargeRounding),
    large = RoundedCornerShape(MediumRounding),
    medium = RoundedCornerShape(SmallRounding),
    small = RoundedCornerShape(TinyRounding),
    extraSmall = RoundedCornerShape(4.dp),
)

fun sunsetColorScheme() = lightColorScheme(
    primary = SunsetPrimary, onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer, onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary, onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    tertiary = SunsetTertiary, onTertiary = SunsetOnTertiary,
    background = SunsetBackground, surface = SunsetSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onBackground = SunsetOnBackground, onSurface = SunsetOnSurface,
    onSurfaceVariant = SunsetOnSurfaceVariant, outline = SunsetOutline,
    error = ErrorRed,
)

fun oceanColorScheme() = lightColorScheme(
    primary = OceanPrimary, onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer, onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary, onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    tertiary = OceanTertiary, onTertiary = OceanOnTertiary,
    background = OceanBackground, surface = OceanSurface,
    surfaceVariant = OceanSurfaceVariant,
    onBackground = OceanOnBackground, onSurface = OceanOnSurface,
    onSurfaceVariant = OceanOnSurfaceVariant, outline = OceanOutline,
    error = ErrorRed,
)

fun cherryColorScheme() = lightColorScheme(
    primary = CherryPrimary, onPrimary = CherryOnPrimary,
    primaryContainer = CherryPrimaryContainer, onPrimaryContainer = CherryOnPrimaryContainer,
    secondary = CherrySecondary, onSecondary = CherryOnSecondary,
    secondaryContainer = CherrySecondaryContainer,
    tertiary = CherryTertiary, onTertiary = CherryOnTertiary,
    background = CherryBackground, surface = CherrySurface,
    surfaceVariant = CherrySurfaceVariant,
    onBackground = CherryOnBackground, onSurface = CherryOnSurface,
    onSurfaceVariant = CherryOnSurfaceVariant, outline = CherryOutline,
    error = ErrorRed,
)

fun forestColorScheme() = lightColorScheme(
    primary = ForestPrimary, onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer, onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary, onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    tertiary = ForestTertiary, onTertiary = ForestOnTertiary,
    background = ForestBackground, surface = ForestSurface,
    surfaceVariant = ForestSurfaceVariant,
    onBackground = ForestOnBackground, onSurface = ForestOnSurface,
    onSurfaceVariant = ForestOnSurfaceVariant, outline = ForestOutline,
    error = ErrorRed,
)

fun lavenderColorScheme() = lightColorScheme(
    primary = LavenderPrimary, onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderPrimaryContainer, onPrimaryContainer = LavenderOnPrimaryContainer,
    secondary = LavenderSecondary, onSecondary = LavenderOnSecondary,
    secondaryContainer = LavenderSecondaryContainer,
    tertiary = LavenderTertiary, onTertiary = LavenderOnTertiary,
    background = LavenderBackground, surface = LavenderSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onBackground = LavenderOnBackground, onSurface = LavenderOnSurface,
    onSurfaceVariant = LavenderOnSurfaceVariant, outline = LavenderOutline,
    error = ErrorRed,
)

fun matchaColorScheme() = lightColorScheme(
    primary = MatchaPrimary, onPrimary = MatchaOnPrimary,
    primaryContainer = MatchaPrimaryContainer, onPrimaryContainer = MatchaOnPrimaryContainer,
    secondary = MatchaSecondary, onSecondary = MatchaOnSecondary,
    secondaryContainer = MatchaSecondaryContainer,
    tertiary = MatchaTertiary, onTertiary = MatchaOnTertiary,
    background = MatchaBackground, surface = MatchaSurface,
    surfaceVariant = MatchaSurfaceVariant,
    onBackground = MatchaOnBackground, onSurface = MatchaOnSurface,
    onSurfaceVariant = MatchaOnSurfaceVariant, outline = MatchaOutline,
    error = ErrorRed,
)

fun midnightColorScheme() = darkColorScheme(
    primary = MidnightPrimary, onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer, onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary, onSecondary = MidnightOnSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = MidnightTertiary, onTertiary = MidnightOnTertiary,
    background = MidnightBackground, surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = MidnightOnBackground, onSurface = MidnightOnSurface,
    onSurfaceVariant = MidnightOnSurfaceVariant, outline = MidnightOutline,
    error = Color(0xFFE0706A),
)

@Composable
fun CampusPalTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorSchemeType: ColorSchemeType = ColorSchemeType.SUNSET,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        isDarkTheme -> midnightColorScheme()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            when {
                isDarkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        else -> when (colorSchemeType) {
            ColorSchemeType.OCEAN -> oceanColorScheme()
            ColorSchemeType.CHERRY -> cherryColorScheme()
            ColorSchemeType.FOREST -> forestColorScheme()
            ColorSchemeType.LAVENDER -> lavenderColorScheme()
            ColorSchemeType.MATCHA -> matchaColorScheme()
            else -> sunsetColorScheme()
        }
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
        shapes = CampusPalShapes,
        typography = Typography,
        content = content,
    )
}
