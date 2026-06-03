package com.example.services

import kotlinx.coroutines.delay
import android.app.*
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.*
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class StrictFocusService : Service() {

    private var secondsLeft = 0
    private var isOverlayShown = false
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var pollingExecutor: ScheduledExecutorService? = null
    private var systemDialogsReceiver: BroadcastReceiver? = null
    private var serviceLifecycleOwner: ServiceLifecycleOwner? = null

    // Track statistics for progressive penalty
    private var emergencyBypassCount = 0
    private var lastEmergencyTime = 0L

    // For Settings sub-blocking (3 seconds consecutive trace list)
    private var consecutiveSettingsSeconds = 0

    // Emergency Bypass active flag (10 seconds window)
    private var isEmergencyBypassActive = false
    private var emergencyEndTime = 0L

    private val prefs by lazy { getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE) }

    companion object {
        const val CHANNEL_ID = "zen_focus_channel"
        const val NOTIFICATION_ID = 9182
        const val ACTION_START = "START_FOCUS"
        const val ACTION_STOP = "STOP_FOCUS"
        const val ACTION_PENALTY_TICK = "ACTION_PENALTY_TICK"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        serviceLifecycleOwner = ServiceLifecycleOwner().apply { start() }

        // System Dialog dialog closing receiver (e.g. intercept when Home or Recent apps is clicked)
        systemDialogsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                    val reason = intent.getStringExtra("reason")
                    if (reason == "homekey" || reason == "recentapps") {
                        // Re-enforce overlay instantly to cover navigation tray
                        if (prefs.getBoolean("tile_active_focus_timer", false) && !isEmergencyBypassActive) {
                            val mainHandler = Handler(Looper.getMainLooper())
                            mainHandler.postDelayed({
                                reassertOverlay()
                            }, 50)
                        }
                    }
                }
            }
        }
        registerReceiver(systemDialogsReceiver, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopFocusSession()
            return START_NOT_STICKY
        }

        // Start session
        val minutes = intent?.getIntExtra("focus_minutes", 25) ?: 25
        secondsLeft = minutes * 60

        // Synchronize state
        prefs.edit()
            .putBoolean("tile_active_focus_timer", true)
            .putInt("focus_seconds_remaining", secondsLeft)
            .apply()

        startForeground(NOTIFICATION_ID, buildNotification(formatTime(secondsLeft)))

        // Toggle DND
        setDndMode(true)

        // Launch Polling Executor Loop (250-400ms interval) -> 300 ms
        pollingExecutor?.shutdownNow()
        pollingExecutor = Executors.newSingleThreadScheduledExecutor()
        pollingExecutor?.scheduleAtFixedRate({
            handlePollingTick()
        }, 0, 300, TimeUnit.MILLISECONDS)

        return START_STICKY
    }

    private fun handlePollingTick() {
        if (secondsLeft <= 0) {
            stopFocusSession()
            return
        }

        // Count seconds remaining (every 1000ms approximate via counter)
        // Since we tick every 300ms, about every 3 ticks we decrement 1 second:
        // We will calculate a precise actual elapsed time instead to be perfectly robust.
        val mainHandler = Handler(Looper.getMainLooper())

        val isFocusEnabled = prefs.getBoolean("tile_active_focus_timer", false)
        if (!isFocusEnabled) {
            mainHandler.post { stopFocusSession() }
            return
        }

        // Check if Emergency Bypass is currently cooling down
        if (isEmergencyBypassActive) {
            val now = System.currentTimeMillis()
            if (now >= emergencyEndTime) {
                isEmergencyBypassActive = false
                prefs.edit().putBoolean("emergency_bypass_active", false).apply()
                consecutiveSettingsSeconds = 0
            } else {
                // In bypass, don't lock
                return
            }
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager != null) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 12000
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            if (!stats.isNullOrEmpty()) {
                val activeApp = stats.sortedBy { it.lastTimeUsed }.lastOrNull()?.packageName
                if (activeApp != null) {
                    processForegroundPackage(activeApp)
                }
            }
        }
    }

    private fun processForegroundPackage(packageName: String) {
        val lower = packageName.lowercase()

        // 1. Check if Whitelisted, Essential, or My Own Package
        if (packageName == this.packageName || isEssentialApp(lower) || isWhitelisted(lower)) {
            // Safe, remove overlay if it is currently displayed
            consecutiveSettingsSeconds = 0
            Handler(Looper.getMainLooper()).post {
                hideOverlay()
            }
            return
        }

        // 2. Settings App Sub-Blocking / Anti-tamper rules
        if (lower.contains("settings")) {
            consecutiveSettingsSeconds++
            // If they are inside settings for > 3 consecutive ticks (~900ms / 3 seconds of check intervals)
            if (consecutiveSettingsSeconds >= 10) { // 10 ticks is ~3 seconds
                // block Settings app info bypass
                Handler(Looper.getMainLooper()).post {
                    showOverlay()
                }
                return
            }
            // Temporarily allow settings briefly for stability, but do not dismiss overlay if already shown
            if (isOverlayShown) {
                // keep locked
            } else {
                return
            }
        } else {
            consecutiveSettingsSeconds = 0
        }

        // 3. Blocked app! Force-render the overlay shielding
        Handler(Looper.getMainLooper()).post {
            showOverlay()
        }
    }

    private fun isEssentialApp(packageName: String): Boolean {
        // Essential system packages must always be allowed to prevent bricking the screen
        val isSystem = packageName.contains("systemui") ||
                packageName.contains("launcher") ||
                packageName.contains("packageinstaller") ||
                packageName.contains("permissioncontroller") ||
                packageName == "android"

        // Also check if home screen
        var isHome = false
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            val homePkg = resolveInfo?.activityInfo?.packageName?.lowercase()
            if (homePkg != null && packageName == homePkg) {
                isHome = true
            }
        } catch (e: Exception) {}

        return isSystem || isHome
    }

    private fun isWhitelisted(packageName: String): Boolean {
        // Read user whitelisted labels from preferences and map
        val mappedName = when {
            packageName.contains("dialer") || packageName.contains("phone") || packageName.contains("telecom") -> "Phone"
            packageName.contains("messaging") || packageName.contains("mms") || packageName.contains("message") -> "Messages"
            packageName.contains("maps") -> "Maps"
            packageName.contains("deskclock") || packageName.contains("clock") -> "Clock"
            packageName.contains("spotify") || packageName.contains("music") -> "Spotify"
            packageName.contains("calculator") -> "Calculator"
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("chrome") || packageName.contains("browser") -> "Chrome"
            else -> null
        }

        if (mappedName != null) {
            val rawList = prefs.getString("whitelisted_apps_csv", "Phone,Messages,Settings,Maps,Clock") ?: ""
            val list = rawList.split(",").map { it.trim() }
            if (list.contains(mappedName)) {
                return true
            }
        }

        // Also, check the banking override safeguard
        val isBank = listOf(
            "bank", "banking", "finance", "payment", "chase", "hsbc", "citibank", "capone",
            "wells_fargo", "wf", "paypal", "gpay", "wallet", "barclays", "bofa", "ally", "revolut",
            "stripe", "venmo", "paytm", "bhim", "upi", "hdfc", "icici", "sbi", "axis", "yono", "cred",
            "cashapp", "robinhood", "coinbase"
        ).any { packageName.contains(it) }

        return isBank
    }

    private fun setDndMode(enable: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (nm != null && nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(
                        if (enable) NotificationManager.INTERRUPTION_FILTER_NONE
                        else NotificationManager.INTERRUPTION_FILTER_ALL
                    )
                }
            } else {
                am?.ringerMode = if (enable) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
            }
        } catch (e: Exception) {}
    }

    private fun showOverlay() {
        if (isOverlayShown) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        overlayView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(serviceLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(serviceLifecycleOwner)

            setContent {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .pointerInput(Unit) {
                                // Block touch propagation to lower layers completely
                                detectTapGestures(onTap = {})
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        OverlayLockContent(
                            secondsLeft = secondsLeft,
                            penaltyTimesUsed = emergencyBypassCount,
                            onEmergencyBypassed = {
                                executeEmergencyBypass()
                            }
                        )
                    }
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            isOverlayShown = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        if (!isOverlayShown) return
        try {
            overlayView?.let { windowManager?.removeView(it) }
            overlayView = null
            isOverlayShown = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun reassertOverlay() {
        // Explicitly force refreshing target layers
        if (isOverlayShown) {
            hideOverlay()
        }
        showOverlay()
    }

    private fun executeEmergencyBypass() {
        emergencyBypassCount++
        isEmergencyBypassActive = true
        emergencyEndTime = System.currentTimeMillis() + 10000 // 10 seconds bypass window

        // Add progressive penalty: if emergency bypass is accessed, penalize focus duration by adding 5 minutes!
        val penaltySecs = 5 * 60
        secondsLeft += penaltySecs
        prefs.edit()
            .putInt("focus_seconds_remaining", secondsLeft)
            .putInt("emergency_bypass_count", emergencyBypassCount)
            .apply()

        // Hide overlay immediately
        hideOverlay()

        Toast.makeText(this, "Emergency Exception: 10s Window Active. Penalty applied (+5m).", Toast.LENGTH_LONG).show()

        // Notify client VM to update list display and logs
        val intent = Intent(ACTION_PENALTY_TICK).apply {
            putExtra("added_seconds", penaltySecs)
            putExtra("count", emergencyBypassCount)
        }
        sendBroadcast(intent)
    }

    private fun stopFocusSession() {
        setDndMode(false)
        hideOverlay()
        pollingExecutor?.shutdownNow()

        prefs.edit()
            .putBoolean("tile_active_focus_timer", false)
            .putInt("focus_seconds_remaining", 0)
            .apply()

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        systemDialogsReceiver?.let { unregisterReceiver(it) }
        serviceLifecycleOwner?.stop()
        pollingExecutor?.shutdownNow()
        hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zen productivity Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Deep Focus Shield notifications to prevent bypasses or system wipes."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(timeStr: String): Notification {
        val stopIntent = Intent(this, StrictFocusService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = Any() // PlaceHolder/Intent builder inside Compose-safe layer below:
        val pendingStopIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("🔒 DEEP ZEN FOCUS ENGAGED")
            .setContentText("Your device is strictly protected. Time Left: $timeStr")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    private val timeTickerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Unused directly; we run scheduling thread directly
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }
}

class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}

@Composable
fun OverlayLockContent(
    secondsLeft: Int,
    penaltyTimesUsed: Int,
    onEmergencyBypassed: () -> Unit
) {
    val m = secondsLeft / 60
    val s = secondsLeft % 60
    val leftStr = String.format("%02d:%02d", m, s)

    var pressProgress by remember { mutableStateOf(0f) }
    var isPressing by remember { mutableStateOf(false) }

    LaunchedEffect(isPressing) {
        if (isPressing) {
            val startTime = System.currentTimeMillis()
            while (isPressing && pressProgress < 1.0f) {
                delay(50)
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed / 3000f).coerceIn(0f, 1f)
            }
            if (pressProgress >= 1.0f) {
                onEmergencyBypassed()
                isPressing = false
                pressProgress = 0f
            }
        } else {
            pressProgress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = Color(0xFFFF1744),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DEEP FOCUS ACTIVE",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Zen Bodyguard has locked system apps to keep you in the zone.",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Timer
        Text(
            text = leftStr,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progressive penalty counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
            Text(
                text = "Emergency Bypass: $penaltyTimesUsed used (+5m penalty each)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFB300)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Emergency Bypass Button (requires 3 seconds hold)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = pressProgress,
                color = Color(0xFFFF1744),
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = if (isPressing) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(180.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressing = true
                                tryAwaitRelease()
                                isPressing = false
                            }
                        )
                    }
            ) {
                Text(
                    text = if (isPressing) "HOLDING (3s)..." else "HOLD TO EXIT (3s)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}
