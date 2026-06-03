package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.models.ThemeState
import com.example.models.ThemeMode

private fun desaturate(color: Color, fraction: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val l = 0.299f * r + 0.587f * g + 0.114f * b // luminance
    return Color(
        red = r + fraction * (l - r),
        green = g + fraction * (l - g),
        blue = b + fraction * (l - b),
        alpha = color.alpha
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = Color(0xFF400000),
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = Color(0xFF93000A),
    onError = md_theme_dark_onError,
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212), // Use dark gray to prevent OLED smearing!
    onBackground = Color(0xDEFFFFFF), // White with 87% opacity (0xDE = 87%)
    surface = Color(0xFF1E1E1E), // Elevate surface slightly
    onSurface = Color(0xDEFFFFFF), // White with 87% opacity
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0x99FFFFFF), // White with 60% opacity (0x99 = 60%)
    outline = Color(0x2BFFFFFF),
    inverseOnSurface = Color.Black,
    inverseSurface = Color.White,
    inversePrimary = Color.LightGray,
    surfaceTint = Color.White,
    outlineVariant = Color(0xFF2C2C2C),
    scrim = Color.Black
)

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFEFEF),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFFFF2A2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color.Black,
    background = Color(0xFFFFFFFF), // pure white for light
    onBackground = Color(0xDE000000), // black with 87% opacity
    surface = Color(0xFFF7F7F7), // slightly greyish surface for light mode
    onSurface = Color(0xDE000000), // black with 87% opacity
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0x99000000), // black with 60% opacity
    outline = Color(0x2B000000),
    inverseOnSurface = Color.White,
    inverseSurface = Color.Black,
    inversePrimary = Color.LightGray,
    surfaceTint = Color.Black,
    outlineVariant = Color(0xFFCCCCCC),
    scrim = Color(0x99000000)
)

@Composable
fun MyApplicationTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    val baseAccent = themeState.accentColor.color
    // Desaturate Brand Accent in dark mode by 25% so it does not visually vibrate
    val accentColor = if (darkTheme) desaturate(baseAccent, 0.25f) else baseAccent

    val colorScheme = if (darkTheme) {
        DarkColorScheme.copy(
            secondary = accentColor,
            onSecondaryContainer = accentColor,
            primary = accentColor
        )
    } else {
        LightColorScheme.copy(
            secondary = accentColor,
            onSecondaryContainer = accentColor,
            primary = accentColor
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
