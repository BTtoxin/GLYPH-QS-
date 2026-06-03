package com.example.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TileSize(val span: Int, val isTall: Boolean = false) {
    SMALL(1, false),       // 1x1
    MEDIUM_WIDE(2, false), // 2x1
    LARGE_SQUARE(2, true), // 2x2
    WIDE(4, false)         // 4x1
}

enum class TileType(val displayName: String, val icon: ImageVector, val defaultSize: TileSize) {
    BATTERY("Battery", Icons.Filled.BatteryChargingFull, TileSize.MEDIUM_WIDE),
    STORAGE("Storage", Icons.Outlined.Storage, TileSize.MEDIUM_WIDE),
    USAGE_STATS("Usage Stats", Icons.Filled.BarChart, TileSize.LARGE_SQUARE),
    FOCUS_TIMER("Deep Focus", Icons.Filled.Timer, TileSize.LARGE_SQUARE),
    FOCUS_SANDBOX("Sandbox Lock", Icons.Filled.Lock, TileSize.SMALL),
    CAFFEINE("Caffeine Keeper", Icons.Filled.LocalCafe, TileSize.SMALL),
    THEATER("Theater Mode", Icons.Filled.Theaters, TileSize.SMALL),
    DESK_LOCK("Desk Lock", Icons.Filled.Desk, TileSize.SMALL),
    WIFI("Wi-Fi", Icons.Filled.Wifi, TileSize.MEDIUM_WIDE),
    BLUETOOTH("Bluetooth", Icons.Filled.Bluetooth, TileSize.SMALL),
    WIFI_SHARE("Wi-Fi Share", Icons.Filled.QrCode, TileSize.SMALL),
    DNS("Network DNS", Icons.Filled.Dns, TileSize.MEDIUM_WIDE),
    TERMINAL("Terminal", Icons.Filled.Terminal, TileSize.WIDE),
    CLIPBOARD("Clear Clipboard", Icons.Filled.ContentPasteOff, TileSize.MEDIUM_WIDE),
    FLASHLIGHT("Flashlight", Icons.Filled.FlashlightOn, TileSize.SMALL),
    SCREEN_TIMEOUT("Timeout", Icons.Filled.ScreenLockPortrait, TileSize.SMALL),
    SHORTCUTS("Shortcuts", Icons.Filled.Link, TileSize.MEDIUM_WIDE),
    GLYPH("Glyph Engine", Icons.Filled.Toll, TileSize.MEDIUM_WIDE),
    COMPASS("Compass", Icons.Filled.Explore, TileSize.SMALL),
    RAM_BOOSTER("Memory Purge", Icons.Filled.Memory, TileSize.MEDIUM_WIDE),
    DECIBEL_METER("Acoustic dB", Icons.Filled.Hearing, TileSize.SMALL),
    MORSE_FLASHER("Morse Code", Icons.Filled.RecordVoiceOver, TileSize.MEDIUM_WIDE),
    SPEED_TEST("Speedbench", Icons.Filled.Speed, TileSize.LARGE_SQUARE),
    STOPWATCH("Precision Sw", Icons.Filled.HourglassTop, TileSize.MEDIUM_WIDE),
    METRONOME("Metronome", Icons.Filled.MusicNote, TileSize.SMALL),
    SOUNDBOARD("O-Synth", Icons.Filled.Piano, TileSize.LARGE_SQUARE),
    PIXEL_ART("Pixel Matrix", Icons.Filled.GridOn, TileSize.LARGE_SQUARE),
    REACTION_TEST("Reflex Game", Icons.Filled.Bolt, TileSize.MEDIUM_WIDE),
    DICE_COIN("Dice & Coin", Icons.Filled.Casino, TileSize.SMALL),
    CPU_TEMP("CPU Thermal", Icons.Filled.Thermostat, TileSize.SMALL),
    PASSWORD_GEN("Keys Forge", Icons.Filled.Key, TileSize.MEDIUM_WIDE),
    WORLD_CLOCK("World Clocks", Icons.Filled.Public, TileSize.MEDIUM_WIDE),
    QUICK_NOTES("Memo Pad", Icons.Filled.Notes, TileSize.LARGE_SQUARE),
    MACRO_EDITOR("Scenario Play", Icons.Filled.PlayCircleOutline, TileSize.LARGE_SQUARE)
}

data class Scenario(
    val id: String,
    val name: String,
    val targetTypes: List<TileType>,
    val hapticPattern: String // "TICK", "RUMBLE", "DOUBLE_TAP", "SWEEP"
)

data class DashboardTile(
    val id: String,
    val type: TileType,
    var size: TileSize,
    var isActive: Boolean = false,
    var displayValue: String? = null // For dynamic info like "85%", "50 GB"
)
