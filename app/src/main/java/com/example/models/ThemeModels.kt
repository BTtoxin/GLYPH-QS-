package com.example.models

import androidx.compose.ui.graphics.Color

enum class BackgroundStyle(val displayName: String) { 
    SOLID_BLACK("Solid Black"), 
    SOLID_DARK_GREY("Dark Grey"), 
    GRADIENT("Gradient") 
}

enum class TileShape(val displayName: String) { 
    ROUNDED("Rounded"), 
    SQUIRCLE("Squircle"), 
    SQUARE("Square") 
}

enum class AccentColorType(val displayName: String, val color: Color) { 
    RED("Nothing Red", Color(0xFFFF2A2A)),
    BLUE("Cyan", Color(0xFF2A8BFF)),
    GREEN("Neon Green", Color(0xFF2AFF6E)),
    YELLOW("Amber", Color(0xFFFFD52A)),
    WHITE("Crisp White", Color(0xFFFFFFFF))
}

data class ThemeState(
    val accentColor: AccentColorType = AccentColorType.RED,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SOLID_BLACK,
    val tileShape: TileShape = TileShape.ROUNDED
)
