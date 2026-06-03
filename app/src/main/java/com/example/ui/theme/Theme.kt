package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NothingWhite,
    secondary = NothingGrey,
    tertiary = NothingRed,
    background = NothingBlack,
    surface = NothingCardBg,
    onPrimary = NothingBlack,
    onSecondary = NothingWhite,
    onTertiary = NothingWhite,
    onBackground = NothingWhite,
    onSurface = NothingWhite,
    outline = NothingBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NothingBlack,
    secondary = NothingGrey,
    tertiary = NothingRed,
    background = NothingWhite,
    surface = Color(0xFFF5F5F5),
    onPrimary = NothingWhite,
    onSecondary = NothingBlack,
    onTertiary = NothingWhite,
    onBackground = NothingBlack,
    onSurface = NothingBlack,
    outline = Color(0xFFDDDDDD)
)

@Composable
fun GlyphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
