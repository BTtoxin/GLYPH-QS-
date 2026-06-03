package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Imports from local subpackages
import com.example.models.DashboardState
import com.example.models.MetricItem
import com.example.models.TrendDirection
import com.example.ui.components.MetricCard
import com.example.ui.components.NothingCSSGrid
import com.example.ui.components.NothingDotMatrixToggle
import com.example.ui.theme.*

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun toggleSetting(key: String) {
        _state.update { current ->
            val updatedToggles = current.toggleSettings.toMutableMap()
            val nextVal = !(updatedToggles[key] ?: false)
            updatedToggles[key] = nextVal

            // Dynamically adjust metrics based on setting changes (interactive telemetry)
            val updatedMetrics = current.metrics.map { metric ->
                when (metric.id) {
                    "cpu" -> {
                        if (key == "BATTERY SAVER" && nextVal) {
                            metric.copy(
                                value = maxOf(30.0, metric.value - 6.5),
                                trend = TrendDirection.NEGATIVE,
                                history = metric.history + (metric.value - 6.5)
                            )
                        } else if (key == "BATTERY SAVER" && !nextVal) {
                            metric.copy(
                                value = minOf(70.0, metric.value + 5.0),
                                trend = TrendDirection.POSITIVE,
                                history = metric.history + (metric.value + 5.0)
                            )
                        } else metric
                    }
                    "ram" -> {
                        if (key == "CAFFEINE KEEPER" && nextVal) {
                            metric.copy(
                                value = minOf(15.5, metric.value + 1.2),
                                trend = TrendDirection.POSITIVE,
                                history = metric.history + (metric.value + 1.2)
                            )
                        } else if (key == "CAFFEINE KEEPER" && !nextVal) {
                            metric.copy(
                                value = maxOf(2.0, metric.value - 0.8),
                                trend = TrendDirection.NEGATIVE,
                                history = metric.history + (metric.value - 0.8)
                            )
                        } else metric
                    }
                    "network" -> {
                        if (key == "5G HIGH SPEED" && nextVal) {
                            metric.copy(
                                value = minOf(1000.0, metric.value + 150.0),
                                trend = TrendDirection.POSITIVE,
                                history = metric.history + (metric.value + 150.0)
                            )
                        } else if (key == "5G HIGH SPEED" && !nextVal) {
                            metric.copy(
                                value = maxOf(50.0, metric.value - 200.0),
                                trend = TrendDirection.NEGATIVE,
                                history = metric.history + (metric.value - 200.0)
                            )
                        } else metric
                    }
                    else -> metric
                }
            }

            current.copy(
                toggleSettings = updatedToggles,
                metrics = updatedMetrics
            )
        }
    }

    fun tickTelemetry() {
        _state.update { current ->
            val batterySaverActive = current.toggleSettings["BATTERY SAVER"] ?: false
            val caffeineKeeperActive = current.toggleSettings["CAFFEINE KEEPER"] ?: false
            val dndActive = current.toggleSettings["DO NOT DISTURB"] ?: false

            val updatedMetrics = current.metrics.map { metric ->
                val delta = (Random.nextDouble() - 0.5) * when (metric.id) {
                    "cpu" -> 1.5
                    "ram" -> 0.15
                    "storage" -> 0.02
                    "network" -> 25.0
                    "battery" -> 0.1
                    "noise" -> 4.0
                    else -> 1.0
                }
                
                var newVal = metric.value + delta
                
                // Bounds enforcement
                newVal = when (metric.id) {
                    "cpu" -> {
                        val base = if (batterySaverActive) 34.0 else 44.0
                        coerceVal(newVal, base - 5.0, base + 20.0)
                    }
                    "ram" -> {
                        val base = if (caffeineKeeperActive) 6.4 else 5.2
                        coerceVal(newVal, 2.5, base + 4.0)
                    }
                    "storage" -> coerceVal(newVal, 60.0, 75.0)
                    "network" -> {
                        val limit = if (current.toggleSettings["5G HIGH SPEED"] == true) 500.0 else 150.0
                        coerceVal(newVal, 10.0, limit + 200.0)
                    }
                    "battery" -> coerceVal(newVal, 1.0, 100.0)
                    "noise" -> {
                        val base = if (dndActive) 30.0 else 45.0
                        coerceVal(newVal, 20.0, base + 30.0)
                    }
                    else -> newVal
                }

                val originalHistory = metric.history
                val newHistory = if (originalHistory.size >= 12) {
                    originalHistory.drop(1) + newVal
                } else {
                    originalHistory + newVal
                }

                val trend = when {
                    newVal > metric.value + 0.01 -> TrendDirection.POSITIVE
                    newVal < metric.value - 0.01 -> TrendDirection.NEGATIVE
                    else -> TrendDirection.NEUTRAL
                }

                metric.copy(
                    value = newVal,
                    trend = trend,
                    history = newHistory
                )
            }
            current.copy(metrics = updatedMetrics)
        }
    }

    private fun coerceVal(v: Double, min: Double, max: Double): Double {
        return maxOf(min, minOf(max, v))
    }

    fun randomizeTelemetry() {
        for (i in 0..5) {
            tickTelemetry()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NothingBlack
                ) {
                    GlyphDashboardScreen()
                }
            }
        }
    }
}

@Composable
fun GlyphDashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedMetric by remember { mutableStateOf<MetricItem?>(null) }
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }
    var activeInfoDialog by remember { mutableStateOf(false) }

    // Tick the clock time live
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        while (true) {
            val calendar = Calendar.getInstance()
            timeString = timeFormat.format(calendar.time)
            dateString = dateFormat.format(calendar.time).toUpperCase(Locale.ROOT)
            delay(1000)
        }
    }

    // Tick the metrics telemetry live to keep the UI engaging as asked by guidelines
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            viewModel.tickTelemetry()
        }
    }

    Scaffold(
        containerColor = NothingBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 4.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Monospace dot matrix header
                        Text(
                            text = timeString,
                            color = NothingWhite,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = dateString,
                            color = NothingGrey,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header actions (Simulated system alert and telemetry trigger)
                        IconButton(
                            onClick = { viewModel.randomizeTelemetry() },
                            modifier = Modifier
                                .border(1.dp, NothingBorder, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Manual Refresh telemetry",
                                tint = NothingWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { activeInfoDialog = true },
                            modifier = Modifier
                                .border(1.dp, NothingBorder, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "System Info",
                                tint = NothingWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = NothingBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            // Elegant brand footer with high contrast design limits
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Small minimalist dot indicators simulating glyph lighting hardware structure
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = if (state.toggleSettings["GLYPH LIGHTS"] == true) NothingRed else NothingBorder)
                        }
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = if (state.toggleSettings["DO NOT DISTURB"] == true) NothingWhite else NothingBorder)
                        }
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = if (state.toggleSettings["BATTERY SAVER"] == true) NothingGrey else NothingBorder)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "GLYPH QS // BY NOTHINGSTYLING",
                        color = NothingGrey,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Toggles Grid Block
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SYSTEM CONTROLS",
                    color = NothingWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Render Quick Settings controllers style toggles cleanly
                Card(
                    colors = CardDefaults.cardColors(containerColor = NothingCardBg),
                    border = BorderStroke(1.2.dp, NothingBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val toggleList = state.toggleSettings.keys.toList()
                        // 2 items per row
                        for (i in toggleList.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for (j in 0..1) {
                                    val index = i + j
                                    if (index < toggleList.size) {
                                        val key = toggleList[index]
                                        val isActive = state.toggleSettings[key] ?: false
                                        
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isActive) Color(0xFF161616) else Color.Transparent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .border(
                                                    if (isActive) BorderStroke(1.dp, NothingWhite.copy(alpha = 0.2f)) 
                                                    else BorderStroke(0.dp, Color.Transparent),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { viewModel.toggleSetting(key) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = key,
                                                        color = if (isActive) NothingWhite else NothingGrey,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    Text(
                                                        text = if (isActive) "ACTIVE" else "OFFLINE",
                                                        color = if (isActive) NothingRed else NothingGrey,
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                                // Dot matrix styled Switch
                                                NothingDotMatrixToggle(
                                                    checked = isActive,
                                                    onCheckedChange = { viewModel.toggleSetting(key) }
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Hardware Live Telemetry Modules
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIVE SYSTEM TELEMETRY",
                    color = NothingWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Layout telemetry cards inside custom responsive grid.
                NothingCSSGrid(
                    columns = 2,
                    spacing = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.metrics.forEach { metric ->
                        item(colSpan = 1) {
                            MetricCard(
                                metric = metric,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedMetric = metric }
                            )
                        }
                    }
                }
            }

            // 3. Simulated Glyph Hardware Light Indicator
            AnimatedVisibility(
                visible = state.toggleSettings["GLYPH LIGHTS"] == true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "GLYPH LIGHTING SIMULATOR",
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NothingCardBg),
                        border = BorderStroke(1.2.dp, NothingBorder),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BACK GLYPH STRIP CONFIG",
                                color = NothingGrey,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Interactive light rings that change dynamically based on state
                            GlyphLightVisualizer(state = state)
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    selectedMetric?.let { metric ->
        MetricDetailDialog(metric = metric, onDismiss = { selectedMetric = null })
    }

    if (activeInfoDialog) {
        InfoDialog(onDismiss = { activeInfoDialog = false })
    }
}

@Composable
fun GlyphLightVisualizer(state: DashboardState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light_pulse"
    )

    val dndActive = state.toggleSettings["DO NOT DISTURB"] ?: false
    val glyphActive = state.toggleSettings["GLYPH LIGHTS"] ?: false
    val batterySaver = state.toggleSettings["BATTERY SAVER"] ?: false

    Canvas(
        modifier = Modifier
            .size(160.dp)
            .padding(8.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.width * 0.45f
        val innerRadius = size.width * 0.32f
        val cameraRadius = size.width * 0.12f

        // Draw structural dark glass backplate guides in dark grey
        drawCircle(
            color = Color(0xFF1E1E1E),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Glyph segment 1: Top-Right Ring Segment (Active always if Glyphs ON, pulses if DND active)
        val segment1Color = if (glyphActive) {
            if (dndActive) NothingRed.copy(alpha = alphaAnim) else NothingWhite
        } else NothingBorder

        drawArc(
            color = segment1Color,
            startAngle = -80f,
            sweepAngle = 70f,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx()),
            size = Size(outerRadius * 2, outerRadius * 2),
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
        )

        // Glyph segment 2: Center-Right Strip Segment (Flashes according to Internet Bandwidth activity)
        val networkRate = state.metrics.firstOrNull { it.id == "network" }?.value ?: 100.0
        val segment2Alpha = if (glyphActive) {
            ((networkRate / 600.0).toFloat().coerceIn(0.2f, 1.0f) * alphaAnim)
        } else 0f
        val segment2Color = if (segment2Alpha > 0f) NothingWhite.copy(alpha = segment2Alpha) else NothingBorder

        drawArc(
            color = segment2Color,
            startAngle = 10f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx()),
            size = Size(outerRadius * 2, outerRadius * 2),
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
        )

        // Glyph segment 3: Bottom Central battery percentage level diagonal indicator bar
        val batteryPct = state.metrics.firstOrNull { it.id == "battery" }?.value ?: 80.0
        val segment3Color = if (glyphActive) {
            if (batterySaver) NothingRed else NothingWhite
        } else NothingBorder

        drawArc(
            color = segment3Color,
            startAngle = 125f,
            sweepAngle = (batteryPct / 100f * 105f).toFloat(),
            useCenter = false,
            style = Stroke(width = 6.dp.toPx()),
            size = Size(outerRadius * 2, outerRadius * 2),
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
        )

        // Glyph segment 4: Inner circle segment around the camera module (Dynamic)
        val segment4Color = if (glyphActive) {
            if (dndActive) NothingBorder else NothingWhite
        } else NothingBorder

        drawCircle(
            color = segment4Color,
            radius = innerRadius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // Camera center island guide
        drawCircle(
            color = Color(0xFF151515),
            radius = cameraRadius,
            center = center
        )
        drawCircle(
            color = NothingBorder,
            radius = cameraRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        // Red dot camera focus pilot light indicator
        drawCircle(
            color = if (glyphActive) NothingRed else Color(0xFF330505),
            radius = 3.dp.toPx(),
            center = Offset(center.x + cameraRadius * 0.4f, center.y - cameraRadius * 0.4f)
        )
    }
}

@Composable
fun MetricDetailDialog(metric: MetricItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NothingCardBg),
            border = BorderStroke(1.2.dp, NothingBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = metric.label,
                    color = NothingWhite,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("%.1f", metric.value),
                        color = NothingWhite,
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = metric.unit,
                        color = NothingGrey,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time custom lightweight Sparkline chart drawn inside Canvas
                Text(
                    text = "12-POINT HISTORY TRACK",
                    color = NothingGrey,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF070707), RoundedCornerShape(12.dp))
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val history = metric.history
                        if (history.size > 1) {
                            val max = history.maxOrNull()?.toFloat() ?: 100f
                            val min = history.minOrNull()?.toFloat() ?: 0f
                            val range = if (max == min) 1f else (max - min)

                            val path = Path()
                            val width = size.width
                            val height = size.height

                            val stepX = width / (history.size - 1)
                            
                            // Let's plot points
                            history.forEachIndexed { idx, valAt ->
                                val x = idx * stepX
                                val y = height - ((valAt.toFloat() - min) / range * height)
                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = NothingWhite,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Highlight current point
                            val lastX = width
                            val lastY = height - ((history.last().toFloat() - min) / range * height)
                            drawCircle(
                                color = NothingRed,
                                radius = 4.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                        } else {
                            // Draw fallback text guide
                            drawLine(
                                color = NothingBorder,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingWhite,
                        contentColor = NothingBlack
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CLOSE MONITOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NothingCardBg),
            border = BorderStroke(1.2.dp, NothingBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "GLYPH QS DEV CONSOLE",
                    color = NothingWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "This utility dashboard replicates the classic 'Nothing OS' aesthetic using ultra-low-power local components. Settings toggles dynamically update telemetry states.",
                    color = NothingGrey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )

                Divider(color = NothingBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GLYPH OS ENG:", color = NothingGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("ACTIVE RES", color = NothingWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CORE COMPILER:", color = NothingGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("KGP 1.9.22", color = NothingWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("AGP CONNECTOR:", color = NothingGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("8.2.2 PROX", color = NothingWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingWhite,
                        contentColor = NothingBlack
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DISMISS CONSOLE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
