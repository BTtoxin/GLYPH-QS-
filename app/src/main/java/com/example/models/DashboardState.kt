package com.example.models

import androidx.compose.runtime.compositionLocalOf

enum class TrendDirection {
    POSITIVE, // Up / increase (e.g. Green or Red depending on preference, we will style monochrome white/accent)
    NEGATIVE, // Down / decrease
    NEUTRAL   // Static / flat
}

data class MetricItem(
    val id: String,
    val value: Double,
    val unit: String,
    val label: String,
    val trend: TrendDirection,
    val history: List<Double> = emptyList(),
    val isSystemCritical: Boolean = false
)

data class DashboardState(
    val toggleSettings: Map<String, Boolean> = mapOf(
        "DO NOT DISTURB" to false,
        "GLYPH LIGHTS" to true,
        "CAFFEINE KEEPER" to false,
        "BATTERY SAVER" to false,
        "5G HIGH SPEED" to true,
        "STRICT DNS SECURE" to true
    ),
    val activeMetrics: Set<String> = setOf("cpu", "ram", "storage", "network"),
    val metrics: List<MetricItem> = listOf(
        MetricItem("cpu", 42.1, "°C", "CPU TEMPERATURE", TrendDirection.POSITIVE, listOf(38.0, 40.2, 41.5, 42.1)),
        MetricItem("ram", 5.8, "GB", "SYSTEM RAM USE", TrendDirection.NEUTRAL, listOf(5.8, 5.8, 5.8, 5.8)),
        MetricItem("storage", 62.1, "%", "STORAGE OCCUPIED", TrendDirection.NEGATIVE, listOf(62.5, 62.4, 62.3, 62.1)),
        MetricItem("network", 341.2, "Mbps", "DL INTERNET BANDWIDTH", TrendDirection.POSITIVE, listOf(290.0, 310.0, 345.0, 341.2)),
        MetricItem("battery", 84.0, "%", "DEVICE BATTERY CAPACITY", TrendDirection.NEGATIVE, listOf(86.0, 85.0, 84.0)),
        MetricItem("noise", 41.5, "dB", "ENVIRONMENT DECIBELS", TrendDirection.NEGATIVE, listOf(48.2, 45.1, 43.0, 41.5))
    )
)

class DashboardActions(
    val onToggleClicked: (String) -> Unit = {},
    val onToggleMetricActive: (String) -> Unit = {},
    val onRefreshMetrics: () -> Unit = {}
)

// The global Compose composition local holders that emulate a React Context Provider:
val LocalDashboardState = compositionLocalOf { DashboardState() }
val LocalDashboardActions = compositionLocalOf { DashboardActions() }
