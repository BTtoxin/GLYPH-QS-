package com.example.viewmodels

import android.app.Application
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.provider.Settings
import android.net.Uri
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import android.os.Vibrator
import android.os.VibrationEffect
import com.example.models.Scenario
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.DashboardTile
import com.example.models.TileSize
import com.example.models.TileType
import com.example.models.ThemeState
import com.example.models.AccentColorType
import com.example.models.BackgroundStyle
import com.example.models.TileShape
import com.example.models.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)

    // Retro synthesizer tone generator
    private var toneGen: ToneGenerator? = null

    // Theme states
    private val _themeState = MutableStateFlow(ThemeState())
    val themeState = _themeState.asStateFlow()

    // Grid columns (2, 3, or 4)
    private val _gridMode = MutableStateFlow(4)
    val gridMode = _gridMode.asStateFlow()

    // Tiles state list
    private val _tiles = MutableStateFlow<List<DashboardTile>>(emptyList())
    val tiles = _tiles.asStateFlow()

    // Edit Mode states
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    // Simulated component failure state
    private val _componentErrorTriggered = MutableStateFlow(false)
    val componentErrorTriggered = _componentErrorTriggered.asStateFlow()

    fun triggerComponentError(triggered: Boolean) {
        _componentErrorTriggered.value = triggered
        if (triggered) {
            com.example.utils.DebugLogger.error("Simulated localized Bento Grid composition failure intercepted and handled with Glyph-styled fallback message.")
        }
    }

    private val _selectedTileIdForSwap = MutableStateFlow<String?>(null)
    val selectedTileIdForSwap = _selectedTileIdForSwap.asStateFlow()

    // Terminal console states
    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("GlyphOS v3.0 Interactive Shell", "Type 'help' for available options."))
    val terminalLogs = _terminalLogs.asStateFlow()

    // Glyph LED engine states
    private val _glyphIntensity = MutableStateFlow(80) // 0-100
    val glyphIntensity = _glyphIntensity.asStateFlow()

    private val _isGlyphBlinking = MutableStateFlow(false)
    val isGlyphBlinking = _isGlyphBlinking.asStateFlow()

    // Caffeine Keeper states
    private val _caffeineOption = MutableStateFlow("Infinite") // or "5 Min", "15 Min"
    val caffeineOption = _caffeineOption.asStateFlow()

    private val _caffeineTimeLeft = MutableStateFlow<String?>(null)
    val caffeineTimeLeft = _caffeineTimeLeft.asStateFlow()

    // Wi-Fi QR Code configurations
    private val _wifiSsid = MutableStateFlow("Nothing_Net_5G")
    val wifiSsid = _wifiSsid.asStateFlow()

    private val _wifiPass = MutableStateFlow("dotmatrix2026")
    val wifiPass = _wifiPass.asStateFlow()

    // Private DNS state
    private val _currentDns = MutableStateFlow("Cloudflare (1.1.1.1)")
    val currentDns = _currentDns.asStateFlow()

    // Map of Tile Custom Names mapped by ID
    private val _customNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val customNames = _customNames.asStateFlow()

    // Map of Tile usage trackers for statistical bar charts.
    private val _useCount = MutableStateFlow<Map<TileType, Int>>(emptyMap())
    val useCount = _useCount.asStateFlow()

    // Simulated desk lock proximity state
    private val _deskLockDistance = MutableStateFlow(3.5f) // simulated distance in meters
    val deskLockDistance = _deskLockDistance.asStateFlow()

    private val _isDeskLockConnected = MutableStateFlow(true)
    val isDeskLockConnected = _isDeskLockConnected.asStateFlow()

    // Sync state for skeleton placeholders
    private val _isSyncing = MutableStateFlow(true)
    val isSyncing = _isSyncing.asStateFlow()

    // App Volume Isolation states
    private val _isolatedAppVolume = MutableStateFlow(65) // simulated percentage
    val isolatedAppVolume = _isolatedAppVolume.asStateFlow()

    // Macro Scenarios flow
    private val _scenarios = MutableStateFlow<List<Scenario>>(emptyList())
    val scenarios = _scenarios.asStateFlow()

    // Simulated screenshot state
    private val _isScreenshotTriggered = MutableStateFlow(false)
    val isScreenshotTriggered = _isScreenshotTriggered.asStateFlow()

    // Global gesture pad overlay active state
    private val _gesturePadActive = MutableStateFlow(false)
    val gesturePadActive = _gesturePadActive.asStateFlow()

    // Sandbox lock active flow representing "Sand Mode"
    private val _sandboxActive = MutableStateFlow(false)
    val sandboxActive = _sandboxActive.asStateFlow()

    // Detailed battery metrics
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel = _batteryLevel.asStateFlow()
    private val _batteryIsCharging = MutableStateFlow(false)
    val batteryIsCharging = _batteryIsCharging.asStateFlow()
    private val _batteryPlugType = MutableStateFlow("Discharging")
    val batteryPlugType = _batteryPlugType.asStateFlow()
    private val _batteryHealth = MutableStateFlow("Good")
    val batteryHealth = _batteryHealth.asStateFlow()
    private val _batteryTemp = MutableStateFlow(32.4f)
    val batteryTemp = _batteryTemp.asStateFlow()
    private val _batteryVoltage = MutableStateFlow(3850)
    val batteryVoltage = _batteryVoltage.asStateFlow()
    private var batteryWarnToastShown = false

    // Magnetic compass sensor fields
    private var sensorManager: android.hardware.SensorManager? = null
    private var compassListener: android.hardware.SensorEventListener? = null
    private var hasPhysicalCompass = false

    // ==========================================
    // MASSIVE EXPANSION PROPERTIES (15+ NEW FEATURES)
    // ==========================================
    
    // 1. Compass Bearing Angle
    private val _compassBearing = MutableStateFlow(84f)
    val compassBearing = _compassBearing.asStateFlow()

    // 2. RAM booster
    private val _ramUsagePercent = MutableStateFlow(68)
    val ramUsagePercent = _ramUsagePercent.asStateFlow()
    private val _isRamCleaning = MutableStateFlow(false)
    val isRamCleaning = _isRamCleaning.asStateFlow()

    // 2b. CPU usage and live history
    private val _cpuUsagePercent = MutableStateFlow(42)
    val cpuUsagePercent = _cpuUsagePercent.asStateFlow()
    private val _cpuHistory = MutableStateFlow<List<Float>>(List(15) { 25f + Random.nextFloat() * 30f })
    val cpuHistory = _cpuHistory.asStateFlow()

    // 2c. System quick toggles state
    private val _isAirplaneModeActive = MutableStateFlow(false)
    val isAirplaneModeActive = _isAirplaneModeActive.asStateFlow()

    // 2d. Generative chat interface (Glyphy via Gemini API)
    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiChatMessages = _aiChatMessages.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // 3. Decibel Noise Meter
    private val _decibelValue = MutableStateFlow(42)
    val decibelValue = _decibelValue.asStateFlow()

    // 4. Morse Code String
    private val _morseText = MutableStateFlow("GLYPH")
    val morseText = _morseText.asStateFlow()
    private val _isMorseFlashing = MutableStateFlow(false)
    val isMorseFlashing = _isMorseFlashing.asStateFlow()

    // 5. Speed Benchmarks
    private val _speedBenchmarkMbps = MutableStateFlow(0f)
    val speedBenchmarkMbps = _speedBenchmarkMbps.asStateFlow()
    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking = _isBenchmarking.asStateFlow()

    // 6. Stopwatch Elapsed system
    private val _stopwatchDisplay = MutableStateFlow("00:00.00")
    val stopwatchDisplay = _stopwatchDisplay.asStateFlow()
    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning = _isStopwatchRunning.asStateFlow()
    private val _stopwatchLaps = MutableStateFlow<List<String>>(emptyList())
    val stopwatchLaps = _stopwatchLaps.asStateFlow()
    private var stopwatchJob: Job? = null
    private var elapsedMs = 0L

    // 7. Metronome Tempo BPM
    private val _metronomeBpm = MutableStateFlow(120)
    val metronomeBpm = _metronomeBpm.asStateFlow()
    private val _isMetronomePlaying = MutableStateFlow(false)
    val isMetronomePlaying = _isMetronomePlaying.asStateFlow()
    private var metronomeJob: Job? = null

    // 8. O-Synth Synthesizer coordinates
    private val _synthPitchX = MutableStateFlow(0.4f)
    val synthPitchX = _synthPitchX.asStateFlow()
    private val _synthFreqY = MutableStateFlow(0.5f)
    val synthFreqY = _synthFreqY.asStateFlow()
    private val _synthWaveform = MutableStateFlow("Square") // Sine, Square, Beep
    val synthWaveform = _synthWaveform.asStateFlow()

    // 9. Pixel Art Matrix 8x8 Grid (64 states)
    private val _pixelGrid = MutableStateFlow(List(64) { false })
    val pixelGrid = _pixelGrid.asStateFlow()

    // 10. Reaction Time Speed Tester Game
    private val _gameStatus = MutableStateFlow("READY") // READY, WAIT, TAP_NOW, SCORED
    val gameStatus = _gameStatus.asStateFlow()
    private val _gameDelayResult = MutableStateFlow<Long?>(null)
    val gameDelayResult = _gameDelayResult.asStateFlow()
    private var gameLaunchTime = 0L

    // 11. Custom Coin/Dice Physics flipper
    private val _diceValue = MutableStateFlow(5)
    val diceValue = _diceValue.asStateFlow()
    private val _coinState = MutableStateFlow("HEADS") // HEADS, TAILS
    val coinState = _coinState.asStateFlow()
    private val _isRollingResult = MutableStateFlow(false)
    val isRollingResult = _isRollingResult.asStateFlow()

    // 12. CPU Thermal Tracker
    private val _cpuTempUnit = MutableStateFlow(41)
    val cpuTempUnit = _cpuTempUnit.asStateFlow()

    // 13. Advanced Custom Password Generator Keys Forge
    private val _passwordResult = MutableStateFlow("GLYPH_SECRET")
    val passwordResult = _passwordResult.asStateFlow()
    private val _passLength = MutableStateFlow(12)
    val passLength = _passLength.asStateFlow()

    // 14. World time clocks
    private val _londonTime = MutableStateFlow("00:00:00")
    val londonTime = _londonTime.asStateFlow()
    private val _tokyoTime = MutableStateFlow("00:00:00")
    val tokyoTime = _tokyoTime.asStateFlow()

    // 15. Quick memo pad notes
    private val _notesSandbox = MutableStateFlow("Nothing is final.\n- Carl Pei")
    val notesSandbox = _notesSandbox.asStateFlow()

    // 16. Custom Deep Focus parameters (1m to 4h) and Whitelist apps
    private val _focusMinutes = MutableStateFlow(25)
    val focusMinutes = _focusMinutes.asStateFlow()

    private val _whitelistedApps = MutableStateFlow(listOf("Phone", "Messages", "Settings", "Maps", "Clock"))
    val whitelistedApps = _whitelistedApps.asStateFlow()

    // Permissions tracking for System-Wide deep focus mode
    private val _overlayPermissionGranted = MutableStateFlow(false)
    val overlayPermissionGranted = _overlayPermissionGranted.asStateFlow()

    private val _usageStatsPermissionGranted = MutableStateFlow(false)
    val usageStatsPermissionGranted = _usageStatsPermissionGranted.asStateFlow()

    private val _dndPermissionGranted = MutableStateFlow(false)
    val dndPermissionGranted = _dndPermissionGranted.asStateFlow()

    // Block statistics to showcase stability / blocking outcomes
    private val _blockedAppsCount = MutableStateFlow(0)
    val blockedAppsCount = _blockedAppsCount.asStateFlow()

    private val _lastBlockedApp = MutableStateFlow<String?>(null)
    val lastBlockedApp = _lastBlockedApp.asStateFlow()

    private var lastBlockedPkg: String? = null
    private var lastBlockTimestamp: Long = 0L

    // Map application package to a friendly name for blocking alert
    fun getFriendlyAppName(pkg: String): String {
        return when {
            pkg.contains("youtube") -> "YouTube"
            pkg.contains("instagram") -> "Instagram"
            pkg.contains("facebook") || pkg.contains("katana") -> "Facebook"
            pkg.contains("tiktok") || pkg.contains("zhiliaoapp") -> "TikTok"
            pkg.contains("twitter") || pkg.contains("twitter") -> "X (Twitter)"
            pkg.contains("messenger") -> "Messenger"
            pkg.contains("telegram") -> "Telegram"
            pkg.contains("snapchat") -> "Snapchat"
            pkg.contains("netflix") -> "Netflix"
            pkg.contains("reddit") -> "Reddit"
            pkg.contains("chrome") -> "Google Chrome"
            pkg.contains("game") || pkg.contains("tencent") || pkg.contains("supercell") || pkg.contains("pubg") || pkg.contains("mojang") -> "Game App"
            else -> pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
        }
    }

    fun incrementBlockedAppsCount(pkg: String) {
        _blockedAppsCount.update { it + 1 }
        _lastBlockedApp.value = getFriendlyAppName(pkg)
    }

    fun hasOverlayPermission(): Boolean {
        val context = getApplication<Application>()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val context = getApplication<Application>()
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                android.app.AppOpsManager.MODE_ALLOWED
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun hasDndPermission(): Boolean {
        val context = getApplication<Application>()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            nm?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun refreshPermissionStates() {
        _overlayPermissionGranted.value = hasOverlayPermission()
        _usageStatsPermissionGranted.value = hasUsageStatsPermission()
        _dndPermissionGranted.value = hasDndPermission()
    }

    fun setFocusMinutes(minutes: Int) {
        val bounded = minutes.coerceIn(1, 240)
        _focusMinutes.value = bounded
        prefs.edit().putInt("focus_minutes", bounded).apply()
        // If focus tile is standby, reset its text representation
        _tiles.update { list ->
            list.map {
                if (it.type == TileType.FOCUS_TIMER && !it.isActive) {
                    it.copy(displayValue = String.format("%02d:00", bounded))
                } else {
                    it
                }
            }
        }
    }

    fun toggleAppWhitelist(appName: String) {
        _whitelistedApps.update { current ->
            val newList = if (current.contains(appName)) {
                if (appName in listOf("Phone", "Messages", "Settings")) {
                    current // Essential services cannot be removed
                } else {
                    current.minus(appName)
                }
            } else {
                current.plus(appName)
            }
            prefs.edit().putString("whitelisted_apps_csv", newList.joinToString(",")).apply()
            newList
        }
    }

    // Private system controllers
    private var focusTimerJob: Job? = null
    private var caffeineTimerJob: Job? = null
    private var simulationJob: Job? = null
    private var isFlashlightOn = false

    // Battery Broadcast Receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isChg = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val plugStr = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> if (isChg) "Charging" else "Discharging"
                }
                
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val healthStr = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                    else -> "Healthy"
                }
                
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320)
                val tempC = tempTenths / 10f
                val voltMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3850)
                
                _batteryLevel.value = pct
                _batteryIsCharging.value = isChg
                _batteryPlugType.value = plugStr
                _batteryHealth.value = healthStr
                _batteryTemp.value = tempC
                _batteryVoltage.value = voltMv
                
                // Low battery warnings threshold < 5%
                if (pct < 5) {
                    if (!batteryWarnToastShown) {
                        Toast.makeText(context ?: getApplication(), "⚠️ CRITICAL POWER STATE: Battery is extremely low ($pct%)!", Toast.LENGTH_LONG).show()
                        batteryWarnToastShown = true
                    }
                } else {
                    batteryWarnToastShown = false
                }
                
                val displayText = if (isChg) "⚡ $pct% (Charging)" else "$pct% Discharging"

                // Update Battery tile info
                _tiles.update { list ->
                    list.map { tile ->
                        if (tile.type == TileType.BATTERY) {
                            tile.copy(displayValue = displayText, isActive = isChg)
                        } else {
                            tile
                        }
                    }
                }
            }
        }
    }

    init {
        // Initial simulated state sync delay to demonstrate beautifully animated skeleton placeholders on screen launch
        viewModelScope.launch {
            delay(2000)
            _isSyncing.value = false
            com.example.utils.DebugLogger.info("Initial engine telemetry & QS state synchronization completed.")
        }
        refreshPermissionStates()
        loadPreferences()
        initializeDefaultTiles()
        registerBatteryReceiver()
        calculateDeviceStorage()
        startLiveSimulations()
        startPhysicalCompass()

        // Fast periodic sync with Widget actions and Quick settings tiles
        viewModelScope.launch {
            while (true) {
                delay(600)
                
                val remoteFocus = prefs.getBoolean("tile_active_focus_timer", false)
                val focusTile = _tiles.value.find { it.type == TileType.FOCUS_TIMER }
                if (focusTile != null) {
                    if (remoteFocus != focusTile.isActive) {
                        _tiles.update { list ->
                            list.map {
                                if (it.type == TileType.FOCUS_TIMER) {
                                    handleFocusTimer(remoteFocus, it)
                                    it.copy(isActive = remoteFocus, displayValue = if (remoteFocus) it.displayValue else String.format("%02d:00", _focusMinutes.value))
                                } else {
                                    it
                                }
                            }
                        }
                    }
                    if (remoteFocus) {
                        val remainingSecs = prefs.getInt("focus_seconds_remaining", 0)
                        if (remainingSecs > 0) {
                            val m = remainingSecs / 60
                            val s = remainingSecs % 60
                            val formatted = String.format("%02d:%02d", m, s)
                            if (focusTile.displayValue != formatted) {
                                updateTileDisplayDirect(TileType.FOCUS_TIMER, formatted)
                            }
                        }
                    }
                }

                if (prefs.getBoolean("tile_active_change_flag", false)) {
                    prefs.edit().putBoolean("tile_active_change_flag", false).apply()
                    val remoteCaffeine = prefs.getBoolean("caffeine_multiplier", false)

                    val caffeineTile = _tiles.value.find { it.type == TileType.CAFFEINE }
                    if (caffeineTile != null && remoteCaffeine != caffeineTile.isActive) {
                        _tiles.update { list ->
                            list.map {
                                if (it.type == TileType.CAFFEINE) {
                                    it.copy(isActive = remoteCaffeine)
                                } else {
                                    it
                                }
                            }
                        }
                    }
                }

                // Keep theme in perfect sync with what's selected by the widget!
                val remoteDark = prefs.getBoolean("is_dark_mode", true)
                val localDark = _themeState.value.isDarkMode
                if (remoteDark != localDark) {
                    _themeState.update { it.copy(isDarkMode = remoteDark) }
                }
            }
        }
    }

    private fun loadPreferences() {
        val accentIndex = prefs.getInt("accent_color", AccentColorType.RED.ordinal)
        val styleIndex = prefs.getInt("bg_style", BackgroundStyle.SOLID_BLACK.ordinal)
        val shapeIndex = prefs.getInt("tile_shape", TileShape.ROUNDED.ordinal)
        val isDarkMode = prefs.getBoolean("is_dark_mode", true)
        val modeIndex = prefs.getInt("theme_mode", ThemeMode.SYSTEM.ordinal)
        val decodedMode = ThemeMode.values().getOrElse(modeIndex) { ThemeMode.SYSTEM }
        _themeState.value = ThemeState(
            accentColor = AccentColorType.values().getOrElse(accentIndex) { AccentColorType.RED },
            backgroundStyle = BackgroundStyle.values().getOrElse(styleIndex) { BackgroundStyle.SOLID_BLACK },
            tileShape = TileShape.values().getOrElse(shapeIndex) { TileShape.ROUNDED },
            isDarkMode = isDarkMode,
            themeMode = decodedMode
        )

        _gridMode.value = prefs.getInt("grid_mode", 4)
        _wifiSsid.value = prefs.getString("wifi_ssid", "Nothing_Net_5G") ?: "Nothing_Net_5G"
        _wifiPass.value = prefs.getString("wifi_pass", "dotmatrix2026") ?: "dotmatrix2026"
        _currentDns.value = prefs.getString("private_dns", "Cloudflare (1.1.1.1)") ?: "Cloudflare (1.1.1.1)"
        _glyphIntensity.value = prefs.getInt("glyph_intensity", 80)

        // Load custom names
        val savedNamesCount = prefs.getInt("custom_names_count", 0)
        val namesMap = mutableMapOf<String, String>()
        for (i in 0 until savedNamesCount) {
            val key = prefs.getString("custom_name_key_$i", null)
            val value = prefs.getString("custom_name_val_$i", null)
            if (key != null && value != null) {
                namesMap[key] = value
            }
        }
        _customNames.value = namesMap

        // Load usage statistics
        val statsMap = mutableMapOf<TileType, Int>()
        TileType.values().forEach { type ->
            val count = prefs.getInt("usage_count_${type.name}", 0)
            statsMap[type] = count
        }
        _useCount.value = statsMap
        _focusMinutes.value = prefs.getInt("focus_minutes", 25)
        val rawSavedList = prefs.getString("whitelisted_apps_csv", "Phone,Messages,Settings,Maps,Clock") ?: "Phone,Messages,Settings,Maps,Clock"
        _whitelistedApps.value = rawSavedList.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        loadScenarios()
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            com.example.utils.DebugLogger.info("Starting diagnostic subsystems and Quick Settings tile state synchronization.")
            delay(1500)
            _isSyncing.value = false
            com.example.utils.DebugLogger.info("Dynamic telemetry sync and panel status verification completed successfully.")
        }
    }

    fun triggerResetAllStates() {
        com.example.utils.DebugLogger.info("Boundary trigger: Performing soft reset of custom layouts and configurations.")
        prefs.edit().clear().apply()
        loadPreferences()
        initializeDefaultTiles()
    }

    private fun saveThemePreferences() {
        prefs.edit().apply {
            putInt("accent_color", _themeState.value.accentColor.ordinal)
            putInt("bg_style", _themeState.value.backgroundStyle.ordinal)
            putInt("tile_shape", _themeState.value.tileShape.ordinal)
            putBoolean("is_dark_mode", _themeState.value.isDarkMode)
            putInt("theme_mode", _themeState.value.themeMode.ordinal)
            apply()
        }
    }

    private fun initializeDefaultTiles() {
        // Build preset list including all 15 new features
        val defaultList = listOf(
            DashboardTile("battery", TileType.BATTERY, TileType.BATTERY.defaultSize, isActive = false, displayValue = "88%"),
            DashboardTile("storage", TileType.STORAGE, TileType.STORAGE.defaultSize, isActive = false, displayValue = "Calculating..."),
            DashboardTile("usage_stats", TileType.USAGE_STATS, TileType.USAGE_STATS.defaultSize, isActive = true),
            DashboardTile("focus_timer", TileType.FOCUS_TIMER, TileType.FOCUS_TIMER.defaultSize, displayValue = "25:00"),
            DashboardTile("focus_sandbox", TileType.FOCUS_SANDBOX, TileType.FOCUS_SANDBOX.defaultSize),
            DashboardTile("caffeine", TileType.CAFFEINE, TileType.CAFFEINE.defaultSize),
            DashboardTile("theater", TileType.THEATER, TileType.THEATER.defaultSize),
            DashboardTile("desk_lock", TileType.DESK_LOCK, TileType.DESK_LOCK.defaultSize),
            DashboardTile("wifi", TileType.WIFI, TileType.WIFI.defaultSize, isActive = true),
            DashboardTile("bluetooth", TileType.BLUETOOTH, TileType.BLUETOOTH.defaultSize, isActive = false),
            DashboardTile("wifi_share", TileType.WIFI_SHARE, TileType.WIFI_SHARE.defaultSize),
            DashboardTile("dns", TileType.DNS, TileType.DNS.defaultSize, displayValue = _currentDns.value),
            DashboardTile("clipboard", TileType.CLIPBOARD, TileType.CLIPBOARD.defaultSize),
            DashboardTile("flashlight", TileType.FLASHLIGHT, TileType.FLASHLIGHT.defaultSize),
            DashboardTile("screen_timeout", TileType.SCREEN_TIMEOUT, TileType.SCREEN_TIMEOUT.defaultSize, displayValue = "30s"),
            DashboardTile("shortcuts", TileType.SHORTCUTS, TileType.SHORTCUTS.defaultSize, displayValue = "Shortcut Mapping"),
            DashboardTile("glyph", TileType.GLYPH, TileType.GLYPH.defaultSize, displayValue = "80% Intensity"),
            DashboardTile("terminal", TileType.TERMINAL, TileType.TERMINAL.defaultSize, displayValue = "user@nothing:~$"),
            
            // New bento features
            DashboardTile("compass", TileType.COMPASS, TileType.COMPASS.defaultSize, displayValue = "84° N"),
            DashboardTile("ram_booster", TileType.RAM_BOOSTER, TileType.RAM_BOOSTER.defaultSize, displayValue = "68% Used"),
            DashboardTile("decibel_meter", TileType.DECIBEL_METER, TileType.DECIBEL_METER.defaultSize, displayValue = "42 dB"),
            DashboardTile("morse_flasher", TileType.MORSE_FLASHER, TileType.MORSE_FLASHER.defaultSize, displayValue = "Morse Ready"),
            DashboardTile("speed_test", TileType.SPEED_TEST, TileType.SPEED_TEST.defaultSize, displayValue = "0.0 Mbps"),
            DashboardTile("stopwatch", TileType.STOPWATCH, TileType.STOPWATCH.defaultSize, displayValue = "00:00.00"),
            DashboardTile("metronome", TileType.METRONOME, TileType.METRONOME.defaultSize, displayValue = "120 BPM"),
            DashboardTile("soundboard", TileType.SOUNDBOARD, TileType.SOUNDBOARD.defaultSize, displayValue = "O-Synth Square"),
            DashboardTile("pixel_art", TileType.PIXEL_ART, TileType.PIXEL_ART.defaultSize, displayValue = "Matrix Active"),
            DashboardTile("reaction_test", TileType.REACTION_TEST, TileType.REACTION_TEST.defaultSize, displayValue = "Not Started"),
            DashboardTile("dice_coin", TileType.DICE_COIN, TileType.DICE_COIN.defaultSize, displayValue = "Ready"),
            DashboardTile("cpu_temp", TileType.CPU_TEMP, TileType.CPU_TEMP.defaultSize, displayValue = "41°C"),
            DashboardTile("password_gen", TileType.PASSWORD_GEN, TileType.PASSWORD_GEN.defaultSize, displayValue = "Keys Active"),
            DashboardTile("world_clock", TileType.WORLD_CLOCK, TileType.WORLD_CLOCK.defaultSize, displayValue = "Global"),
            DashboardTile("quick_notes", TileType.QUICK_NOTES, TileType.QUICK_NOTES.defaultSize, displayValue = "Memo Active"),
            DashboardTile("macro_editor", TileType.MACRO_EDITOR, TileType.MACRO_EDITOR.defaultSize, displayValue = "Scenarios Active")
        )

        // Read or write default ordering
        val savedOrder = prefs.getString("tiles_ordering", null)
        if (savedOrder != null) {
            val ids = savedOrder.split(",")
            val mapped = ids.mapNotNull { id ->
                defaultList.find { it.id == id }
            }.toMutableList()
            // Add any missing default tiles in case of additions
            defaultList.forEach { def ->
                if (mapped.none { it.id == def.id }) {
                    mapped.add(def)
                }
            }
            _tiles.value = mapped
        } else {
            _tiles.value = defaultList
            persistTileOrder(defaultList)
        }
    }

    private fun persistTileOrder(list: List<DashboardTile>) {
        val order = list.joinToString(",") { it.id }
        prefs.edit().putString("tiles_ordering", order).apply()
    }

    private fun registerBatteryReceiver() {
        try {
            val context = getApplication<Application>()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Since ACTION_BATTERY_CHANGED is a system broadcast, wait, is it possible to register as RECEIVER_NOT_EXPORTED? Yes.
                // Or if it needs to be exported, but Android 14 says: "system broadcasts can be registered with RECEIVER_NOT_EXPORTED or RECEIVER_EXPORTED".
                // To be safe, we register as RECEIVER_NOT_EXPORTED
                val flag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Context.RECEIVER_NOT_EXPORTED
                } else {
                    0
                }
                context.registerReceiver(
                    batteryReceiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    flag
                )
            } else {
                context.registerReceiver(
                    batteryReceiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateDeviceStorage() {
        viewModelScope.launch {
            val stats = getStorageStatsInGb()
            _tiles.update { list ->
                list.map { tile ->
                    if (tile.type == TileType.STORAGE) {
                        tile.copy(displayValue = "${stats.first} GB Free / ${stats.second} GB")
                    } else {
                        tile
                    }
                }
            }
        }
    }

    private suspend fun getStorageStatsInGb(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val freeGb = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
            val totalGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
            Pair(freeGb, totalGb)
        } catch (e: Exception) {
            Pair(45L, 128L)
        }
    }

    fun updateThemeState(newState: ThemeState) {
        _themeState.value = newState
        saveThemePreferences()
    }

    fun toggleThemeMode() {
        val current = _themeState.value
        _themeState.value = current.copy(isDarkMode = !current.isDarkMode)
        saveThemePreferences()
        playTickTone(ToneGenerator.TONE_PROP_BEEP)
    }

    fun setGridMode(columns: Int) {
        _gridMode.value = columns
        prefs.edit().putInt("grid_mode", columns).apply()
    }

    fun toggleEditMode() {
        _isEditMode.update { !it }
        _selectedTileIdForSwap.value = null
    }

    // Swaps or selects tile in edit mode
    fun handleTileClickInEditMode(tileId: String) {
        val currentSelect = _selectedTileIdForSwap.value
        if (currentSelect == null) {
            _selectedTileIdForSwap.value = tileId
        } else if (currentSelect == tileId) {
            _selectedTileIdForSwap.value = null // deselect
        } else {
            // Swap them!
            swapTiles(currentSelect, tileId)
            _selectedTileIdForSwap.value = null
        }
    }

    private fun swapTiles(idA: String, idB: String) {
        _tiles.update { list ->
            val updated = list.toMutableList()
            val indexA = updated.indexOfFirst { it.id == idA }
            val indexB = updated.indexOfFirst { it.id == idB }
            if (indexA != -1 && indexB != -1) {
                val temp = updated[indexA]
                updated[indexA] = updated[indexB]
                updated[indexB] = temp
            }
            persistTileOrder(updated)
            updated
        }
    }

    fun resizeTile(tileId: String, newSize: TileSize) {
        _tiles.update { list ->
            val updated = list.map { tile ->
                if (tile.id == tileId) tile.copy(size = newSize) else tile
            }
            updated
        }
    }

    fun renameTile(tileId: String, name: String) {
        val updatedMap = _customNames.value.toMutableMap()
        updatedMap[tileId] = name
        _customNames.value = updatedMap

        // Persist renamed keys
        val editor = prefs.edit()
        editor.putInt("custom_names_count", updatedMap.size)
        var i = 0
        updatedMap.forEach { (k, v) ->
            editor.putString("custom_name_key_$i", k)
            editor.putString("custom_name_val_$i", v)
            i++
        }
        editor.apply()
    }

    fun triggerTileAction(tileId: String) {
        val application = getApplication<Application>()
        
        // Track usage stats
        val currentList = _tiles.value
        val clickedTile = currentList.find { it.id == tileId }
        if (clickedTile != null) {
            recordUsage(clickedTile.type)
        }

        _tiles.update { list ->
            list.map { tile ->
                if (tile.id == tileId) {
                    val newState = !tile.isActive
                    playTickTone(ToneGenerator.TONE_PROP_BEEP)
                    playHapticVibration("TICK")
                    when (tile.type) {
                        TileType.FOCUS_TIMER -> {
                            handleFocusTimer(newState, tile)
                            prefs.edit().putBoolean("tile_active_focus_timer", newState).apply()
                            tile.copy(isActive = newState)
                        }
                        TileType.SCREEN_TIMEOUT -> {
                            val timeoutStr = tile.displayValue ?: "30s"
                            val timeoutMs = when (timeoutStr) {
                                "15s" -> 15 * 1000
                                "30s" -> 30 * 1000
                                "1m" -> 60 * 1000
                                "5m" -> 5 * 60 * 1000
                                "10m" -> 10 * 60 * 1000
                                else -> 30 * 1000
                            }
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    if (android.provider.Settings.System.canWrite(application)) {
                                        android.provider.Settings.System.putInt(
                                            application.contentResolver,
                                            android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                                            timeoutMs
                                        )
                                        Toast.makeText(application, "System Screen Timeout configured to $timeoutStr", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(application, "Timeout simulated: $timeoutStr. (Grant system write settings permission to apply globally)", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    android.provider.Settings.System.putInt(
                                        application.contentResolver,
                                        android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                                        timeoutMs
                                    )
                                    Toast.makeText(application, "Screen Timeout configured to $timeoutStr", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(application, "Timeout simulated: $timeoutStr", Toast.LENGTH_SHORT).show()
                            }
                            tile.copy(isActive = true)
                        }
                        TileType.CLIPBOARD -> {
                            val clipManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipManager.setPrimaryClip(ClipData.newPlainText("", ""))
                            Toast.makeText(application, "Sensitive Clipboard Purged", Toast.LENGTH_SHORT).show()
                            tile.copy(isActive = true)
                            viewModelScope.launch {
                                delay(1200)
                                _tiles.update { listSub ->
                                    listSub.map { if (it.id == tileId) it.copy(isActive = false) else it }
                                }
                            }
                            tile
                        }
                        TileType.FLASHLIGHT -> {
                            toggleFlashlight(application, newState)
                            tile.copy(isActive = newState)
                        }
                        TileType.CAFFEINE -> {
                            handleCaffeineLock(newState)
                            if (newState) {
                                Toast.makeText(application, "Caffeine Mode SUCCESS: Screen Lock Engaged (${_caffeineOption.value})", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(application, "Caffeine Mode DISABLED: Screen Lock Released", Toast.LENGTH_SHORT).show()
                            }
                            tile.copy(isActive = newState)
                        }
                        TileType.GLYPH -> {
                            triggerGlyphBlink(newState)
                            tile.copy(isActive = newState)
                        }
                        TileType.THEATER -> {
                            val am = application.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                            if (newState) {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        if (android.provider.Settings.System.canWrite(application)) {
                                            android.provider.Settings.System.putInt(
                                                application.contentResolver,
                                                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                                10
                                            )
                                        }
                                    } else {
                                        android.provider.Settings.System.putInt(
                                            application.contentResolver,
                                            android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                            10
                                        )
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                                
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        if (nm != null && nm.isNotificationPolicyAccessGranted) {
                                            am?.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
                                        }
                                    } else {
                                        am?.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                                
                                Toast.makeText(application, "Theater Mode active: Ultra Dim Display + Ringer Muted", Toast.LENGTH_SHORT).show()
                            } else {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        if (android.provider.Settings.System.canWrite(application)) {
                                            android.provider.Settings.System.putInt(
                                                application.contentResolver,
                                                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                                140
                                            )
                                        }
                                    } else {
                                        android.provider.Settings.System.putInt(
                                            application.contentResolver,
                                            android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                            140
                                        )
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                                
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        if (nm != null && nm.isNotificationPolicyAccessGranted) {
                                            am?.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                                        }
                                    } else {
                                        am?.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                                
                                Toast.makeText(application, "Theater Mode inactive. Brightness & Audio restored", Toast.LENGTH_SHORT).show()
                            }
                            tile.copy(isActive = newState)
                        }
                        TileType.FOCUS_SANDBOX -> {
                            _sandboxActive.value = newState
                            if (newState) {
                                Toast.makeText(application, "Deep Sand Sandbox engaged. Screen pinned. (Hold Back+Overview to exit)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(application, "Sandbox released.", Toast.LENGTH_SHORT).show()
                            }
                            tile.copy(isActive = newState)
                        }
                        TileType.RAM_BOOSTER -> {
                            purgeMemoryBoooster()
                            tile.copy(isActive = true)
                        }
                        TileType.SPEED_TEST -> {
                            runSpeedBenchmark()
                            tile.copy(isActive = true)
                        }
                        TileType.STOPWATCH -> {
                            toggleStopwatch()
                            tile.copy(isActive = newState)
                        }
                        TileType.METRONOME -> {
                            toggleMetronome()
                            tile.copy(isActive = newState)
                        }
                        TileType.REACTION_TEST -> {
                            if (_gameStatus.value == "TAP_NOW") {
                                tapReactionTrigger()
                            } else {
                                startReactionGame()
                            }
                            tile.copy(isActive = true)
                        }
                        TileType.DICE_COIN -> {
                            rollDiceAndCoin()
                            tile.copy(isActive = true)
                        }
                        TileType.PASSWORD_GEN -> {
                            generatePassword()
                            tile.copy(isActive = true)
                        }
                        TileType.MORSE_FLASHER -> {
                            triggerMorseFlasher()
                            tile.copy(isActive = true)
                        }
                        TileType.WIFI -> {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    val intent = android.content.Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    application.startActivity(intent)
                                } else {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    application.startActivity(intent)
                                }
                                Toast.makeText(application, "Opening System Wi-Fi panel", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            tile.copy(isActive = newState)
                        }
                        TileType.BLUETOOTH -> {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                application.startActivity(intent)
                                Toast.makeText(application, "Opening System Bluetooth settings", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            tile.copy(isActive = newState)
                        }
                        else -> tile.copy(isActive = newState)
                    }
                } else {
                    tile
                }
            }
        }
    }

    private fun recordUsage(type: TileType) {
        val currentMap = _useCount.value.toMutableMap()
        val prev = currentMap[type] ?: 0
        currentMap[type] = prev + 1
        _useCount.value = currentMap

        prefs.edit().putInt("usage_count_${type.name}", prev + 1).apply()
    }

    private fun toggleFlashlight(context: Context, state: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0)
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, state)
                isFlashlightOn = state
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isBankingApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        val bankKeywords = listOf(
            "bank", "banking", "finance", "payment", "chase", "hsbc", "citibank", "capone",
            "wells_fargo", "wf", "paypal", "gpay", "wallet", "barclays", "bofa", "ally", "revolut",
            "stripe", "venmo", "paytm", "bhim", "upi", "hdfc", "icici", "sbi", "axis", "yono", "cred",
            "cashapp", "robinhood", "coinbase"
        )
        return bankKeywords.any { lower.contains(it) }
    }

    fun isProductiveApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        val context = getApplication<Application>()
        
        // Essential system components are ALWAYS allowed to prevent soft locking the device
        if (lower.contains("systemui") || 
            lower.contains("launcher") || 
            lower.contains("packageinstaller") || 
            lower.contains("permissioncontroller") ||
            lower == "android"
        ) {
            return true
        }

        // Dynamically allow the active home screen (launcher) to prevent soft locking
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            val launcherPackage = resolveInfo?.activityInfo?.packageName?.lowercase()
            if (launcherPackage != null && lower == launcherPackage) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Resolve friendly mapping which users can toggle in their whitelisting UI
        val mappedName = when {
            lower.contains("dialer") || lower.contains("phone") || lower.contains("telecom") -> "Phone"
            lower.contains("messaging") || lower.contains("mms") || lower.contains("message") -> "Messages"
            lower.contains("settings") -> "Settings"
            lower.contains("maps") -> "Maps"
            lower.contains("deskclock") || lower.contains("clock") -> "Clock"
            lower.contains("spotify") || lower.contains("music") -> "Spotify"
            lower.contains("calculator") -> "Calculator"
            lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("youtube") -> "YouTube"
            lower.contains("chrome") || lower.contains("browser") -> "Chrome"
            else -> null
        }
        
        if (mappedName != null) {
            return _whitelistedApps.value.contains(mappedName)
        }
        
        return false
    }

    fun launchLockoutScreen(context: Context, blockedPkg: String) {
        try {
            val intent = Intent(context, Class.forName("com.example.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SYSTEM_BLOCK_TRIGGERED", true)
                putExtra("BLOCKED_PACKAGE_NAME", blockedPkg)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleFocusTimer(isActive: Boolean, tile: DashboardTile) {
        val application = getApplication<Application>()
        try {
            val intent = Intent(application, com.example.services.StrictFocusService::class.java).apply {
                action = if (isActive) com.example.services.StrictFocusService.ACTION_START else com.example.services.StrictFocusService.ACTION_STOP
                putExtra("focus_minutes", _focusMinutes.value)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCaffeineLock(isActive: Boolean) {
        caffeineTimerJob?.cancel()
        _caffeineTimeLeft.value = null
        if (isActive) {
            val option = _caffeineOption.value
            if (option != "Infinite") {
                val durationInMins = when (option) {
                    "5 Min" -> 5
                    "15 Min" -> 15
                    "30 Min" -> 30
                    else -> 5
                }
                var secsLeft = durationInMins * 60
                caffeineTimerJob = viewModelScope.launch {
                    while (secsLeft > 0) {
                        delay(1000)
                        secsLeft--
                        val mins = secsLeft / 60
                        val secs = secsLeft % 60
                        _caffeineTimeLeft.value = String.format("%02d:%02d", mins, secs)
                    }
                    _caffeineTimeLeft.value = null
                    // deactivate
                    _tiles.update { list ->
                        list.map { if (it.type == TileType.CAFFEINE) it.copy(isActive = false) else it }
                    }
                }
            }
        }
    }

    fun setCaffeineOption(option: String) {
        _caffeineOption.value = option
        val caffeineTile = _tiles.value.find { it.type == TileType.CAFFEINE }
        if (caffeineTile?.isActive == true) {
            handleCaffeineLock(true)
        }
    }

    private var glyphBlinkJob: Job? = null

    private fun triggerGlyphBlink(isActive: Boolean) {
        glyphBlinkJob?.cancel()
        _isGlyphBlinking.value = isActive
        if (isActive) {
            val application = getApplication<Application>()
            glyphBlinkJob = viewModelScope.launch(Dispatchers.Default) {
                try {
                    val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                    val cameraId = cameraManager.cameraIdList.getOrNull(0)
                    if (cameraId != null) {
                        var flashState = false
                        while (_isGlyphBlinking.value) {
                            flashState = !flashState
                            cameraManager.setTorchMode(cameraId, flashState)
                            delay(180)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                        val cameraId = cameraManager.cameraIdList.getOrNull(0)
                        if (cameraId != null) {
                            cameraManager.setTorchMode(cameraId, false)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    fun updateGlyphIntensity(intensity: Int) {
        _glyphIntensity.value = intensity
        prefs.edit().putInt("glyph_intensity", intensity).apply()
        _tiles.update { list ->
            list.map { if (it.type == TileType.GLYPH) it.copy(displayValue = "$intensity% Intensity") else it }
        }
    }

    fun setWifiConfiguration(ssid: String, pass: String) {
        _wifiSsid.value = ssid
        _wifiPass.value = pass
        prefs.edit().apply {
            putString("wifi_ssid", ssid)
            putString("wifi_pass", pass)
            apply()
        }
    }

    fun setPrivateDns(dns: String) {
        _currentDns.value = dns
        prefs.edit().putString("private_dns", dns).apply()
        _tiles.update { list ->
            list.map { if (it.type == TileType.DNS) it.copy(displayValue = dns) else it }
        }
    }

    fun startPhysicalCompass() {
        val application = getApplication<Application>()
        try {
            val sm = application.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager ?: return
            sensorManager = sm
            
            val sensor = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
            if (sensor != null) {
                compassListener = object : android.hardware.SensorEventListener {
                    private val rotationMatrix = FloatArray(9)
                    private val orientationAngles = FloatArray(3)
                    
                    override fun onSensorChanged(event: android.hardware.SensorEvent) {
                        if (event.sensor.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                            android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            android.hardware.SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val azimuthRad = orientationAngles[0]
                            var degrees = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                            if (degrees < 0) {
                                degrees += 360f
                            }
                            hasPhysicalCompass = true
                            _compassBearing.value = degrees
                            updateCompassDisplay(degrees)
                        }
                    }
                    override fun onAccuracyChanged(s: android.hardware.Sensor?, accuracy: Int) {}
                }
                sm.registerListener(compassListener, sensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
            } else {
                val magSensor = sm.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)
                val accSensor = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
                if (magSensor != null && accSensor != null) {
                    compassListener = object : android.hardware.SensorEventListener {
                        private val gravity = FloatArray(3)
                        private val geomagnetic = FloatArray(3)
                        private val R = FloatArray(9)
                        private val I = FloatArray(9)
                        private val orientation = FloatArray(3)
                        
                        override fun onSensorChanged(event: android.hardware.SensorEvent) {
                            if (event.sensor.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                                System.arraycopy(event.values, 0, gravity, 0, 3)
                            } else if (event.sensor.type == android.hardware.Sensor.TYPE_MAGNETIC_FIELD) {
                                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            }
                            
                            if (android.hardware.SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                                android.hardware.SensorManager.getOrientation(R, orientation)
                                val azimuthRad = orientation[0]
                                var degrees = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                                if (degrees < 0) {
                                    degrees += 360f
                                }
                                hasPhysicalCompass = true
                                _compassBearing.value = degrees
                                updateCompassDisplay(degrees)
                            }
                        }
                        override fun onAccuracyChanged(s: android.hardware.Sensor?, accuracy: Int) {}
                    }
                    sm.registerListener(compassListener, magSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
                    sm.registerListener(compassListener, accSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPhysicalCompass() {
        try {
            compassListener?.let {
                sensorManager?.unregisterListener(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        compassListener = null
    }

    private fun updateCompassDisplay(bearing: Float) {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val dirStr = dirs[(((bearing + 22.5f) % 360f) / 45f).toInt()]
        updateTileDisplayDirect(TileType.COMPASS, String.format("%.0f° %s", bearing, dirStr))
    }

    fun openSystemWriteSettings() {
        val application = getApplication<Application>()
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:" + application.packageName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
            Toast.makeText(application, "Opening System Write Settings", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(application, "Settings not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDisplaySettings() {
        val application = getApplication<Application>()
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
            Toast.makeText(application, "Opening Display & Timeout settings", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(application, "Display setting intent failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDndSettings() {
        val application = getApplication<Application>()
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
                Toast.makeText(application, "Grant Do Not Disturb / Zen policy access", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(application, "DND access not required on this Android version", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(application, "Notification settings failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openBatterySaverSettings() {
        val application = getApplication<Application>()
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
            Toast.makeText(application, "Opening Battery Saver settings", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(application, "Battery saver settings failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openScreenPinningSettings() {
        val application = getApplication<Application>()
        try {
            val intent = android.content.Intent("android.settings.SCREEN_PINNING_SETTINGS").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
            Toast.makeText(application, "Opening System Screen Pinning options", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
                Toast.makeText(application, "Opening System Security Settings", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Toast.makeText(application, "Screen pinning settings failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openPrivateDnsSettings() {
        val application = getApplication<Application>()
        try {
            val intent = android.content.Intent("android.settings.PRIVATE_DNS_SETTINGS").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
            Toast.makeText(application, "Opening System Private DNS panels", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
                Toast.makeText(application, "Opening Network settings (Private DNS located inside)", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(application, "Network settings failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resetAllToFactoryDefaults() {
        val application = getApplication<Application>()
        try {
            // 1. Clear SharedPreferences
            prefs.edit().clear().apply()
            
            // 2. Shut down ongoing mode runtimes
            caffeineTimerJob?.cancel()
            _caffeineTimeLeft.value = null
            focusTimerJob?.cancel()
            
            // 3. Reset local properties
            _sandboxActive.value = false
            _isGlyphBlinking.value = false
            _currentDns.value = "Cloudflare (1.1.1.1)"
            _wifiSsid.value = "Nothing_Net_5G"
            _wifiPass.value = "dotmatrix2026"
            _glyphIntensity.value = 80
            
            // 4. Reload defaults & reinitialize presets
            loadPreferences()
            
            val defaultList = listOf(
                DashboardTile("battery", TileType.BATTERY, TileType.BATTERY.defaultSize, isActive = false, displayValue = "88%"),
                DashboardTile("storage", TileType.STORAGE, TileType.STORAGE.defaultSize, isActive = false, displayValue = "Calculating..."),
                DashboardTile("usage_stats", TileType.USAGE_STATS, TileType.USAGE_STATS.defaultSize, isActive = true),
                DashboardTile("focus_timer", TileType.FOCUS_TIMER, TileType.FOCUS_TIMER.defaultSize, displayValue = "25:00"),
                DashboardTile("focus_sandbox", TileType.FOCUS_SANDBOX, TileType.FOCUS_SANDBOX.defaultSize),
                DashboardTile("caffeine", TileType.CAFFEINE, TileType.CAFFEINE.defaultSize),
                DashboardTile("theater", TileType.THEATER, TileType.THEATER.defaultSize),
                DashboardTile("desk_lock", TileType.DESK_LOCK, TileType.DESK_LOCK.defaultSize),
                DashboardTile("wifi", TileType.WIFI, TileType.WIFI.defaultSize, isActive = true),
                DashboardTile("bluetooth", TileType.BLUETOOTH, TileType.BLUETOOTH.defaultSize, isActive = false),
                DashboardTile("wifi_share", TileType.WIFI_SHARE, TileType.WIFI_SHARE.defaultSize),
                DashboardTile("dns", TileType.DNS, TileType.DNS.defaultSize, displayValue = _currentDns.value),
                DashboardTile("clipboard", TileType.CLIPBOARD, TileType.CLIPBOARD.defaultSize),
                DashboardTile("flashlight", TileType.FLASHLIGHT, TileType.FLASHLIGHT.defaultSize),
                DashboardTile("screen_timeout", TileType.SCREEN_TIMEOUT, TileType.SCREEN_TIMEOUT.defaultSize, displayValue = "30s"),
                DashboardTile("shortcuts", TileType.SHORTCUTS, TileType.SHORTCUTS.defaultSize, displayValue = "Shortcut Mapping"),
                DashboardTile("glyph", TileType.GLYPH, TileType.GLYPH.defaultSize, displayValue = "80% Intensity"),
                DashboardTile("terminal", TileType.TERMINAL, TileType.TERMINAL.defaultSize, displayValue = "user@nothing:~$"),
                DashboardTile("compass", TileType.COMPASS, TileType.COMPASS.defaultSize, displayValue = "84° N"),
                DashboardTile("ram_booster", TileType.RAM_BOOSTER, TileType.RAM_BOOSTER.defaultSize, displayValue = "68% Used"),
                DashboardTile("decibel_meter", TileType.DECIBEL_METER, TileType.DECIBEL_METER.defaultSize, displayValue = "42 dB"),
                DashboardTile("morse_flasher", TileType.MORSE_FLASHER, TileType.MORSE_FLASHER.defaultSize, displayValue = "Morse Ready"),
                DashboardTile("speed_test", TileType.SPEED_TEST, TileType.SPEED_TEST.defaultSize, displayValue = "0.0 Mbps"),
                DashboardTile("stopwatch", TileType.STOPWATCH, TileType.STOPWATCH.defaultSize, displayValue = "00:00.00"),
                DashboardTile("metronome", TileType.METRONOME, TileType.METRONOME.defaultSize, displayValue = "120 BPM"),
                DashboardTile("soundboard", TileType.SOUNDBOARD, TileType.SOUNDBOARD.defaultSize, displayValue = "O-Synth Square"),
                DashboardTile("pixel_art", TileType.PIXEL_ART, TileType.PIXEL_ART.defaultSize, displayValue = "Matrix Active"),
                DashboardTile("reaction_test", TileType.REACTION_TEST, TileType.REACTION_TEST.defaultSize, displayValue = "Not Started"),
                DashboardTile("dice_coin", TileType.DICE_COIN, TileType.DICE_COIN.defaultSize, displayValue = "Ready"),
                DashboardTile("cpu_temp", TileType.CPU_TEMP, TileType.CPU_TEMP.defaultSize, displayValue = "41°C"),
                DashboardTile("password_gen", TileType.PASSWORD_GEN, TileType.PASSWORD_GEN.defaultSize, displayValue = "Keys Active"),
                DashboardTile("world_clock", TileType.WORLD_CLOCK, TileType.WORLD_CLOCK.defaultSize, displayValue = "Global"),
                DashboardTile("quick_notes", TileType.QUICK_NOTES, TileType.QUICK_NOTES.defaultSize, displayValue = "Memo Active"),
                DashboardTile("macro_editor", TileType.MACRO_EDITOR, TileType.MACRO_EDITOR.defaultSize, displayValue = "Scenarios Active")
            )
            _tiles.value = defaultList
            persistTileOrder(defaultList)
            
            Toast.makeText(application, "Reset all custom Glyphs & system mode configurations to factory defaults", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(application, "Reset operation encountered an error", Toast.LENGTH_SHORT).show()
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val raw = cmd.trim()
        if (raw.isEmpty()) return

        val newLogs = _terminalLogs.value.toMutableList()
        newLogs.add("> $raw")

        val args = raw.split(" ")
        val baseCmd = args[0].lowercase()

        when (baseCmd) {
            "help" -> {
                newLogs.add("Available commands:")
                newLogs.add("  help      - List commands")
                newLogs.add("  systat    - Quick storage size & network status details")
                newLogs.add("  dns <val> - Configure private DNS (e.g. google, adguard, cloudflare)")
                newLogs.add("  caffeine  - Print current Wake-Lock countdown profile")
                newLogs.add("  theme     - Print layout & visual bento styles")
                newLogs.add("  glyph <val>- Set rear simulated LED intensity (0-100)")
                newLogs.add("  clear     - Clears the prompt logs")
            }
            "clear" -> {
                newLogs.clear()
                newLogs.add("Shell cleared. user@nothing:~$")
            }
            "systat" -> {
                newLogs.add("Device Storage: Calculated via StatFs.")
                val list = _tiles.value
                val storage = list.find { it.type == TileType.STORAGE }?.displayValue ?: "Unknown"
                val battery = list.find { it.type == TileType.BATTERY }?.displayValue ?: "Unknown"
                newLogs.add("  Disk: $storage")
                newLogs.add("  Power: $battery")
                newLogs.add("  Private DNS: ${_currentDns.value}")
                newLogs.add("  Grid Format: ${_gridMode.value}xN")
            }
            "dns" -> {
                if (args.size < 2) {
                    newLogs.add("Usage: dns <google|cloudflare|adguard|custom>")
                } else {
                    val chosen = args[1].lowercase()
                    val formatted = when (chosen) {
                        "google" -> "Google DNS (8.8.8.8)"
                        "cloudflare" -> "Cloudflare (1.1.1.1)"
                        "adguard" -> "AdGuard (76.76.19.19)"
                        else -> "Custom ($chosen)"
                    }
                    setPrivateDns(formatted)
                    newLogs.add("DNS mapped: $formatted")
                }
            }
            "caffeine" -> {
                val dur = _caffeineOption.value
                val active = _tiles.value.find { it.type == TileType.CAFFEINE }?.isActive == true
                newLogs.add("Caffeine Keeper Profile:")
                newLogs.add("  State: " + if (active) "Active (Wake-lock engaged)" else "Inactive")
                newLogs.add("  Duration: $dur")
                _caffeineTimeLeft.value?.let { newLogs.add("  Remaining: $it") }
            }
            "theme" -> {
                val state = _themeState.value
                newLogs.add("Visual Theme Specification:")
                newLogs.add("  Accent color: ${state.accentColor.displayName}")
                newLogs.add("  Background Style: ${state.backgroundStyle.name}")
                newLogs.add("  Bento Rounding: ${state.tileShape.name}")
            }
            "glyph" -> {
                if (args.size < 2) {
                    newLogs.add("Glyph Engine status:")
                    newLogs.add("  Intensity: ${_glyphIntensity.value}%")
                    newLogs.add("  Simulation flashing: " + if (_isGlyphBlinking.value) "ON" else "OFF")
                } else {
                    val value = args[1].toIntOrNull()
                    if (value == null || value !in 0..100) {
                        newLogs.add("Intensity must be an integer from 0 to 100")
                    } else {
                        updateGlyphIntensity(value)
                        newLogs.add("Simulated rear LED intensity updated to $value%")
                    }
                }
            }
            else -> {
                newLogs.add("Command error: '$baseCmd' is unrecognized. Type 'help'.")
            }
        }
        _terminalLogs.value = newLogs
    }

    fun setDeskLockDistance(dist: Float) {
        _deskLockDistance.value = dist
    }

    fun setDeskLockConnected(connected: Boolean) {
        _isDeskLockConnected.value = connected
        // Auto toggles desk lock tile state to active the distance locks
        _tiles.update { list ->
            list.map { if (it.type == TileType.DESK_LOCK) it.copy(isActive = !connected) else it }
        }
    }

    fun setAppVolume(volume: Int) {
        _isolatedAppVolume.value = volume
    }

    private fun updateTileDisplay(id: String, value: String) {
        _tiles.update { list ->
            list.map { if (it.id == id) it.copy(displayValue = value) else it }
        }
    }

    // ==========================================
    // MASSIVE EXPANSION BUSINESS LOGIC IMPLEMENTATIONS
    // ==========================================

    private fun startLiveSimulations() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var degree = 84f
            val r = Random(System.currentTimeMillis())
            while (true) {
                delay(1500)
                // 1. Compass slight rotation simulation
                if (!hasPhysicalCompass) {
                    degree = (degree + r.nextInt(-4, 5) + 360f) % 360f
                    _compassBearing.value = degree
                    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                    val dirStr = dirs[(((degree + 22.5f) % 360f) / 45f).toInt()]
                    updateTileDisplayDirect(TileType.COMPASS, String.format("%.0f° %s", degree, dirStr))
                }

                // 2. Decibel noise meter fluctuations simulation
                val db = r.nextInt(35, 76)
                _decibelValue.value = db
                updateTileDisplayDirect(TileType.DECIBEL_METER, "$db dB")

                // 3. CPU Thermal slight variations simulation
                val temp = r.nextInt(38, 46)
                _cpuTempUnit.value = temp
                updateTileDisplayDirect(TileType.CPU_TEMP, "$temp°C")

                // Maintain CPU usage fluctuations and historical tick array
                val nextCpu = 12f + r.nextFloat() * 56f
                _cpuUsagePercent.value = nextCpu.toInt()
                _cpuHistory.update { history ->
                    history.drop(1) + nextCpu
                }

                // 4. World Clocks live state ticking updates
                val nowSecs = System.currentTimeMillis() / 1000
                val formatClock = { offsetHrs: Int ->
                    val totalSecs = nowSecs + offsetHrs * 3600
                    val h = (totalSecs / 3600 % 24)
                    val m = (totalSecs / 60 % 60)
                    val s = (totalSecs % 60)
                    String.format("%02d:%02d:%02d", h, m, s)
                }
                _londonTime.value = formatClock(1) // London Timezone Offset (UTC+1)
                _tokyoTime.value = formatClock(9)  // Tokyo Timezone Offset (UTC+9)
            }
        }
    }

    private fun updateTileDisplayDirect(type: TileType, value: String) {
        _tiles.update { list ->
            list.map { if (it.type == type) it.copy(displayValue = value) else it }
        }
    }

    fun playTickTone(pitch: Int = ToneGenerator.TONE_PROP_BEEP) {
        try {
            if (toneGen == null) {
                toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            }
            toneGen?.startTone(pitch, 50)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // A. RAM memory cleaner tool
    fun purgeMemoryBoooster() {
        if (_isRamCleaning.value) return
        _isRamCleaning.value = true
        playTickTone(ToneGenerator.TONE_DTMF_D)
        viewModelScope.launch {
            updateTileDisplayDirect(TileType.RAM_BOOSTER, "Purging Caches...")
            for (p in 68 downTo 32) {
                _ramUsagePercent.value = p
                delay(40)
            }
            _isRamCleaning.value = false
            playTickTone(ToneGenerator.TONE_DTMF_0)
            updateTileDisplayDirect(TileType.RAM_BOOSTER, "32% (Optimized)")
            Toast.makeText(getApplication(), "Freed 2.4 GB of background cache!", Toast.LENGTH_SHORT).show()
        }
    }

    // B. Morse flasher matrix transmitter
    fun setMorseMessage(msg: String) {
        _morseText.value = msg.uppercase()
        updateTileDisplayDirect(TileType.MORSE_FLASHER, "Morse: ${msg.uppercase()}")
    }

    fun triggerMorseFlasher() {
        if (_isMorseFlashing.value) return
        _isMorseFlashing.value = true
        val application = getApplication<Application>()
        viewModelScope.launch {
            val codeMap = mapOf(
                'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
                'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
                'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
                'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
                'Y' to "-.--", 'Z' to "--..", '1' to ".----", '2' to "..---", '3' to "...--",
                '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..",
                '9' to "----.", '0' to "-----"
            )
            val currentMsg = _morseText.value
            for (char in currentMsg) {
                val code = codeMap[char] ?: continue
                for (sym in code) {
                    toggleFlashlight(application, true)
                    playTickTone(ToneGenerator.TONE_PROP_BEEP)
                    delay(if (sym == '.') 200 else 500)
                    toggleFlashlight(application, false)
                    delay(200)
                }
                delay(500) // Letter space delay
            }
            _isMorseFlashing.value = false
        }
    }

    // C. Network Speedbench Benchmarking
    fun runSpeedBenchmark() {
        if (_isBenchmarking.value) return
        _isBenchmarking.value = true
        playTickTone(ToneGenerator.TONE_DTMF_2)
        viewModelScope.launch {
            updateTileDisplayDirect(TileType.SPEED_TEST, "Testing Ping...")
            delay(1000)
            val r = Random(System.currentTimeMillis())
            for (i in 0..15) {
                val sp = 120f + r.nextFloat() * 180f
                _speedBenchmarkMbps.value = sp
                updateTileDisplayDirect(TileType.SPEED_TEST, String.format("%.1f Mbps", sp))
                playTickTone(ToneGenerator.TONE_PROP_BEEP2)
                delay(120)
            }
            _isBenchmarking.value = false
            Toast.makeText(getApplication(), "Download: ${_speedBenchmarkMbps.value} Mbps", Toast.LENGTH_SHORT).show()
        }
    }

    // D. Precise stopwatch counting
    fun toggleStopwatch() {
        if (_isStopwatchRunning.value) {
            _isStopwatchRunning.value = false
            stopwatchJob?.cancel()
            playTickTone(ToneGenerator.TONE_PROP_BEEP2)
        } else {
            _isStopwatchRunning.value = true
            playTickTone(ToneGenerator.TONE_PROP_BEEP)
            stopwatchJob = viewModelScope.launch {
                val start = System.currentTimeMillis() - elapsedMs
                while (_isStopwatchRunning.value) {
                    elapsedMs = System.currentTimeMillis() - start
                    val mins = (elapsedMs / 60000) % 60
                    val secs = (elapsedMs / 1000) % 60
                    val hund = (elapsedMs / 10) % 100
                    val disp = String.format("%02d:%02d.%02d", mins, secs, hund)
                    _stopwatchDisplay.value = disp
                    updateTileDisplayDirect(TileType.STOPWATCH, disp)
                    delay(30)
                }
            }
        }
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        elapsedMs = 0L
        _stopwatchDisplay.value = "00:00.00"
        _stopwatchLaps.value = emptyList()
        updateTileDisplayDirect(TileType.STOPWATCH, "00:00.00")
        playTickTone(ToneGenerator.TONE_DTMF_0)
    }

    fun lapStopwatch() {
        val currentDisp = _stopwatchDisplay.value
        val list = _stopwatchLaps.value.toMutableList()
        list.add("Lap ${list.size + 1}: $currentDisp")
        _stopwatchLaps.value = list
        playTickTone(ToneGenerator.TONE_PROP_BEEP)
    }

    // E. Metronome tapping strobe
    fun toggleMetronome() {
        if (_isMetronomePlaying.value) {
            _isMetronomePlaying.value = false
            metronomeJob?.cancel()
            playTickTone(ToneGenerator.TONE_DTMF_5)
        } else {
            _isMetronomePlaying.value = true
            metronomeJob = viewModelScope.launch {
                while (_isMetronomePlaying.value) {
                    playTickTone(ToneGenerator.TONE_PROP_BEEP)
                    val interval = (60000 / _metronomeBpm.value).toLong()
                    delay(interval)
                }
            }
        }
    }

    fun setMetronomeBpm(bpm: Int) {
        _metronomeBpm.value = bpm
        updateTileDisplayDirect(TileType.METRONOME, "$bpm BPM")
    }

    // F. O-Synth synthesizer coordinates
    fun updateSynthCoords(x: Float, y: Float) {
        _synthPitchX.value = x
        _synthFreqY.value = y
        val freqTone = ToneGenerator.TONE_PROP_BEEP
        playTickTone(freqTone)
    }

    fun setSynthWaveform(wave: String) {
        _synthWaveform.value = wave
        updateTileDisplayDirect(TileType.SOUNDBOARD, "Synth: $wave")
    }

    // G. Pixel painting board grid editor
    fun togglePixelIndex(index: Int) {
        val list = _pixelGrid.value.toMutableList()
        if (index in 0 until 64) {
            list[index] = !list[index]
            _pixelGrid.value = list
            playTickTone(ToneGenerator.TONE_PROP_BEEP)
        }
    }

    fun clearPixelGrid() {
        _pixelGrid.value = List(64) { false }
        playTickTone(ToneGenerator.TONE_DTMF_0)
    }

    // H. Reaction response testing game
    fun startReactionGame() {
        _gameStatus.value = "WAIT"
        _gameDelayResult.value = null
        updateTileDisplayDirect(TileType.REACTION_TEST, "Wait for red...")
        playTickTone(ToneGenerator.TONE_DTMF_1)
        viewModelScope.launch {
            val randomDelay = 1500 + Random.nextLong(2000)
            delay(randomDelay)
            if (_gameStatus.value == "WAIT") {
                _gameStatus.value = "TAP_NOW"
                gameLaunchTime = System.currentTimeMillis()
                updateTileDisplayDirect(TileType.REACTION_TEST, "TAP NOW!")
                playTickTone(ToneGenerator.TONE_DTMF_9)
            }
        }
    }

    fun tapReactionTrigger() {
        if (_gameStatus.value == "WAIT") {
            _gameStatus.value = "READY"
            updateTileDisplayDirect(TileType.REACTION_TEST, "Foul Tap!")
            Toast.makeText(getApplication(), "Tapped too early!", Toast.LENGTH_SHORT).show()
            playTickTone(ToneGenerator.TONE_DTMF_D)
        } else if (_gameStatus.value == "TAP_NOW") {
            val diff = System.currentTimeMillis() - gameLaunchTime
            _gameStatus.value = "SCORED"
            _gameDelayResult.value = diff
            updateTileDisplayDirect(TileType.REACTION_TEST, "$diff ms")
            playTickTone(ToneGenerator.TONE_PROP_BEEP2)
        }
    }

    fun resetReactionGame() {
        _gameStatus.value = "READY"
        _gameDelayResult.value = null
        updateTileDisplayDirect(TileType.REACTION_TEST, "Ready")
        playTickTone(ToneGenerator.TONE_DTMF_0)
    }

    // I. Dice coin random roller physics
    fun rollDiceAndCoin() {
        if (_isRollingResult.value) return
        _isRollingResult.value = true
        playTickTone(ToneGenerator.TONE_DTMF_A)
        viewModelScope.launch {
            for (i in 0..6) {
                _diceValue.value = Random.nextInt(1, 7)
                _coinState.value = if (Random.nextBoolean()) "HEADS" else "TAILS"
                delay(100)
            }
            _isRollingResult.value = false
            updateTileDisplayDirect(TileType.DICE_COIN, "D:${_diceValue.value} / C:${_coinState.value}")
            playTickTone(ToneGenerator.TONE_PROP_BEEP2)
        }
    }

    // J. Keys Forge advanced passwords setting
    fun setPassLength(len: Int) {
        _passLength.value = len
        generatePassword()
    }

    fun generatePassword() {
        val alph = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%"
        val sb = StringBuilder()
        val len = _passLength.value
        val r = Random(System.currentTimeMillis())
        for (i in 0 until len) {
            sb.append(alph[r.nextInt(alph.length)])
        }
        val gen = sb.toString()
        _passwordResult.value = gen
        updateTileDisplayDirect(TileType.PASSWORD_GEN, "Forge: $gen")
        playTickTone(ToneGenerator.TONE_PROP_BEEP)
    }

    // K. Quick memo notes sandbox saver
    fun updateQuickNotes(content: String) {
        _notesSandbox.value = content
    }

    // ==========================================
    // L. MICRO LABS DESIGN FEATURES (MACROS, GESTURES, SCENARIOS)
    // ==========================================

    private fun loadScenarios() {
        val serialized = prefs.getString("custom_scenarios", null)
        val list = mutableListOf<Scenario>()
        if (serialized == null) {
            // Baseline presets
            list.add(Scenario("preset_1", "Late Cinema Mode", listOf(TileType.THEATER, TileType.CAFFEINE, TileType.SCREEN_TIMEOUT), "DOUBLE_TAP"))
            list.add(Scenario("preset_2", "Extreme Game Boost", listOf(TileType.RAM_BOOSTER, TileType.WIFI, TileType.GLYPH), "RUMBLE"))
            list.add(Scenario("preset_3", "Zen Deep Study", listOf(TileType.FOCUS_TIMER, TileType.METRONOME), "SWEEP"))
            saveScenariosList(list)
        } else {
            try {
                val parts = serialized.split(";")
                for (part in parts) {
                    if (part.isBlank()) continue
                    val sub = part.split("|")
                    if (sub.size >= 4) {
                        val id = sub[0]
                        val name = sub[1]
                        val typesStr = sub[2]
                        val haptic = sub[3]
                        val types = if (typesStr.isBlank()) emptyList() else typesStr.split(",").mapNotNull {
                            try { TileType.valueOf(it) } catch (e: Exception) { null }
                        }
                        list.add(Scenario(id, name, types, haptic))
                    }
                }
            } catch (e: Exception) {
                list.add(Scenario("preset_1", "Late Cinema Mode", listOf(TileType.THEATER, TileType.CAFFEINE, TileType.SCREEN_TIMEOUT), "DOUBLE_TAP"))
            }
        }
        _scenarios.value = list
    }

    private fun saveScenariosList(list: List<Scenario>) {
        val sb = StringBuilder()
        for (sc in list) {
            val typesStr = sc.targetTypes.joinToString(",") { it.name }
            sb.append("${sc.id}|${sc.name}|$typesStr|${sc.hapticPattern};")
        }
        prefs.edit().putString("custom_scenarios", sb.toString()).apply()
    }

    fun addScenario(name: String, targetTypes: List<TileType>, hapticPattern: String) {
        val newSc = Scenario("sc_" + UUID.randomUUID().toString().take(6), name, targetTypes, hapticPattern)
        val current = _scenarios.value.toMutableList()
        current.add(newSc)
        _scenarios.value = current
        saveScenariosList(current)
        Toast.makeText(getApplication(), "Scenario '$name' Created!", Toast.LENGTH_SHORT).show()
        playHapticVibration("TICK")
    }

    fun deleteScenario(id: String) {
        val current = _scenarios.value.filter { it.id != id }
        _scenarios.value = current
        saveScenariosList(current)
        Toast.makeText(getApplication(), "Scenario Deleted", Toast.LENGTH_SHORT).show()
        playHapticVibration("TICK")
    }

    fun playHapticVibration(pattern: String) {
        val context = getApplication<Application>()
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    when (pattern) {
                        "TICK" -> vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                        "RUMBLE" -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100, 50, 100), -1))
                        "DOUBLE_TAP" -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1))
                        "SWEEP" -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 80, 50, 120), -1))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    when (pattern) {
                        "TICK" -> vibrator.vibrate(80)
                        "RUMBLE" -> vibrator.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
                        "DOUBLE_TAP" -> vibrator.vibrate(longArrayOf(0, 80, 80, 80), -1)
                        "SWEEP" -> vibrator.vibrate(longArrayOf(0, 50, 50, 80, 50, 120), -1)
                    }
                }
            } catch (e: Exception) {
                try {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                } catch (ex: Exception) {}
            }
        }
        // Play synthesizer audio feedback synchronized
        when (pattern) {
            "TICK" -> playTickTone(ToneGenerator.TONE_PROP_BEEP)
            "RUMBLE" -> {
                viewModelScope.launch {
                    playTickTone(ToneGenerator.TONE_DTMF_D)
                    delay(100)
                    playTickTone(ToneGenerator.TONE_DTMF_0)
                }
            }
            "DOUBLE_TAP" -> {
                viewModelScope.launch {
                    playTickTone(ToneGenerator.TONE_PROP_BEEP2)
                    delay(120)
                    playTickTone(ToneGenerator.TONE_PROP_BEEP2)
                }
            }
            "SWEEP" -> {
                viewModelScope.launch {
                    playTickTone(ToneGenerator.TONE_PROP_BEEP)
                    delay(100)
                    playTickTone(ToneGenerator.TONE_PROP_BEEP2)
                    delay(100)
                    playTickTone(ToneGenerator.TONE_DTMF_9)
                }
            }
        }
    }

    fun runScenario(scenario: Scenario) {
        playHapticVibration(scenario.hapticPattern)
        val application = getApplication<Application>()
        viewModelScope.launch {
            Toast.makeText(application, "Starting: ${scenario.name}", Toast.LENGTH_SHORT).show()
            for (type in scenario.targetTypes) {
                val tile = _tiles.value.find { it.type == type }
                if (tile != null) {
                    triggerTileAction(tile.id)
                    delay(300)
                }
            }
            Toast.makeText(application, "${scenario.name} Fully Deployed", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleGesturePad() {
        _gesturePadActive.value = !_gesturePadActive.value
        playHapticVibration("TICK")
        if (_gesturePadActive.value) {
            Toast.makeText(getApplication(), "Gesture Overlay active. Draw O / I or 2-finger tap background!", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerGestureAction(gestureName: String) {
        val application = getApplication<Application>()
        when (gestureName) {
            "Circle O" -> {
                val flashTile = _tiles.value.find { it.type == TileType.FLASHLIGHT }
                if (flashTile != null) {
                    triggerTileAction(flashTile.id)
                } else {
                    toggleFlashlight(application, true)
                }
                playHapticVibration("DOUBLE_TAP")
                Toast.makeText(application, "Circle 'O' Drawn: Flashlight triggered!", Toast.LENGTH_SHORT).show()
            }
            "Line I" -> {
                purgeMemoryBoooster()
                Toast.makeText(application, "Line 'I' Drawn: Memory purged!", Toast.LENGTH_SHORT).show()
            }
            "Screenshot" -> {
                _isScreenshotTriggered.value = true
                playHapticVibration("SWEEP")
                viewModelScope.launch {
                    delay(600)
                    _isScreenshotTriggered.value = false
                }
                Toast.makeText(application, "System Screen Mapping Saved to Gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resetScreenshot() {
        _isScreenshotTriggered.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPhysicalCompass()
        focusTimerJob?.cancel()
        caffeineTimerJob?.cancel()
        simulationJob?.cancel()
        stopwatchJob?.cancel()
        metronomeJob?.cancel()
        try {
            toneGen?.release()
            toneGen = null
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setAirplaneModeActive(active: Boolean) {
        _isAirplaneModeActive.value = active
        playTickTone(ToneGenerator.TONE_PROP_BEEP2)
        val app = getApplication<Application>()
        Toast.makeText(app, if (active) "Airplane Mode Enabled" else "Airplane Mode Disabled", Toast.LENGTH_SHORT).show()
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage("user", text)
        _aiChatMessages.update { it + userMsg }
        _isAiLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val responseText = callGeminiApi(text)
            _aiChatMessages.update { it + ChatMessage("gemini", responseText) }
            _isAiLoading.value = false
            playTickTone(ToneGenerator.TONE_PROP_BEEP)
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Glyphy is ready! To start chatting, please set your Gemini API Key in the AI Studio Secrets panel."
        }

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val systemInstruction = "You are Glyphy, the official Nothing OS style Glyph Dashboard AI companion. " +
                "Provide brief, ultra-concise system diagnostics, productivity summaries or control suggestions. " +
                "Be extremely direct and bulleted, keeping answers under 3 lines. Use monochrome accents and subtle technical tone."

        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "${prompt.replace("\"", "\\\"").replace("\n", "\\n")}"
                    }
                  ]
                }
              ],
              "systemInstruction": {
                "parts": [
                  {
                    "text": "$systemInstruction"
                  }
                ]
              }
            }
        """.trimIndent()

        val body = okhttp3.RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonPayload
        )

        val request = okhttp3.Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val textStart = responseBody.indexOf("\"text\": \"")
                    if (textStart != -1) {
                        val actualStart = textStart + 9
                        val textEnd = responseBody.indexOf("\"", actualStart)
                        if (textEnd != -1) {
                            val rawText = responseBody.substring(actualStart, textEnd)
                            rawText
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                        } else {
                            "Unable to parse response."
                        }
                    } else {
                        "No text generated."
                    }
                } else {
                    "Error code: HTTP ${response.code}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Glyphy timed out. Please check your network connection: ${e.localizedMessage}"
        }
    }
}

data class ChatMessage(
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
