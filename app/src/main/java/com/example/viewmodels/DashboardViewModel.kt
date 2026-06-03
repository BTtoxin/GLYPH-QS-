package com.example.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.DashboardTile
import com.example.models.TileSize
import com.example.models.TileType
import com.example.models.ThemeState
import com.example.models.AccentColorType
import com.example.models.BackgroundStyle
import com.example.models.TileShape
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

    // App Volume Isolation states
    private val _isolatedAppVolume = MutableStateFlow(65) // simulated percentage
    val isolatedAppVolume = _isolatedAppVolume.asStateFlow()

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
        loadPreferences()
        initializeDefaultTiles()
        registerBatteryReceiver()
        calculateDeviceStorage()
        startLiveSimulations()
    }

    private fun loadPreferences() {
        val accentIndex = prefs.getInt("accent_color", AccentColorType.RED.ordinal)
        val styleIndex = prefs.getInt("bg_style", BackgroundStyle.SOLID_BLACK.ordinal)
        val shapeIndex = prefs.getInt("tile_shape", TileShape.ROUNDED.ordinal)
        _themeState.value = ThemeState(
            accentColor = AccentColorType.values().getOrElse(accentIndex) { AccentColorType.RED },
            backgroundStyle = BackgroundStyle.values().getOrElse(styleIndex) { BackgroundStyle.SOLID_BLACK },
            tileShape = TileShape.values().getOrElse(shapeIndex) { TileShape.ROUNDED }
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
    }

    private fun saveThemePreferences() {
        prefs.edit().apply {
            putInt("accent_color", _themeState.value.accentColor.ordinal)
            putInt("bg_style", _themeState.value.backgroundStyle.ordinal)
            putInt("tile_shape", _themeState.value.tileShape.ordinal)
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
            DashboardTile("quick_notes", TileType.QUICK_NOTES, TileType.QUICK_NOTES.defaultSize, displayValue = "Memo Active")
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
            getApplication<Application>().registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
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
                    when (tile.type) {
                        TileType.FOCUS_TIMER -> {
                            handleFocusTimer(newState, tile)
                            tile.copy(isActive = newState)
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
                            tile.copy(isActive = newState)
                        }
                        TileType.GLYPH -> {
                            triggerGlyphBlink(newState)
                            tile.copy(isActive = newState)
                        }
                        TileType.THEATER -> {
                            if (newState) {
                                Toast.makeText(application, "Theater Mode Macro Triggered - Brightness Low, DND Simulator active", Toast.LENGTH_SHORT).show()
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
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, state)
            isFlashlightOn = state
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleFocusTimer(isActive: Boolean, tile: DashboardTile) {
        focusTimerJob?.cancel()
        if (isActive) {
            var timeLeft = 25 * 60
            focusTimerJob = viewModelScope.launch {
                while (timeLeft > 0) {
                    delay(1000)
                    timeLeft--
                    val mins = timeLeft / 60
                    val secs = timeLeft % 60
                    updateTileDisplay(tile.id, String.format("%02d:%02d", mins, secs))
                }
                updateTileDisplay(tile.id, "25:00")
                _tiles.update { list -> list.map { if (it.id == tile.id) it.copy(isActive = false) else it } }
            }
        } else {
            updateTileDisplay(tile.id, "25:00")
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

    private fun triggerGlyphBlink(isActive: Boolean) {
        if (isActive) {
            _isGlyphBlinking.value = true
        } else {
            _isGlyphBlinking.value = false
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
                degree = (degree + r.nextInt(-4, 5) + 360f) % 360f
                _compassBearing.value = degree
                val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                val dirStr = dirs[(((degree + 22.5f) % 360f) / 45f).toInt()]
                updateTileDisplayDirect(TileType.COMPASS, String.format("%.0f° %s", degree, dirStr))

                // 2. Decibel noise meter fluctuations simulation
                val db = r.nextInt(35, 76)
                _decibelValue.value = db
                updateTileDisplayDirect(TileType.DECIBEL_METER, "$db dB")

                // 3. CPU Thermal slight variations simulation
                val temp = r.nextInt(38, 46)
                _cpuTempUnit.value = temp
                updateTileDisplayDirect(TileType.CPU_TEMP, "$temp°C")

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
        } catch (e: Exception) {
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

    override fun onCleared() {
        super.onCleared()
        focusTimerJob?.cancel()
        caffeineTimerJob?.cancel()
        simulationJob?.cancel()
        stopwatchJob?.cancel()
        metronomeJob?.cancel()
        try {
            toneGen?.release()
            toneGen = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
