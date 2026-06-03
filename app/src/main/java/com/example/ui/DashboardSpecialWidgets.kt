package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.ThemeState
import com.example.viewmodels.DashboardViewModel
import com.example.viewmodels.ChatMessage

@Composable
fun DotMatrixIcon(
    iconType: String,
    active: Boolean,
    activeColor: Color = Color.Black,
    inactiveColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val icon = when (iconType) {
        "WIFI" -> Icons.Filled.Wifi
        "BLUETOOTH" -> Icons.Filled.Bluetooth
        "AIRPLANE" -> Icons.Filled.AirplanemodeActive
        "DARKMODE" -> Icons.Filled.DarkMode
        else -> Icons.Filled.Settings
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Subtle dotted matrix grid in the background of the icon to preserve Nothing's custom aesthetic
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / 5
            val cellH = size.height / 5
            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    drawCircle(
                        color = if (active) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                        radius = 1.2.dp.toPx(),
                        center = Offset(c * cellW + cellW / 2, r * cellH + cellH / 2)
                    )
                }
            }
        }

        // Beautiful, distinguishable High-Visibility central icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) activeColor else inactiveColor,
            modifier = Modifier.fillMaxSize(0.55f)
        )
    }
}

@Composable
fun QuickSettingsPanel(
    viewModel: DashboardViewModel,
    themeState: ThemeState,
    modifier: Modifier = Modifier
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val airplaneActive by viewModel.isAirplaneModeActive.collectAsStateWithLifecycle()

    val wifiTile = remember(tiles) { tiles.find { it.type == com.example.models.TileType.WIFI } }
    val wifiActive = wifiTile?.isActive == true

    val btTile = remember(tiles) { tiles.find { it.type == com.example.models.TileType.BLUETOOTH } }
    val btActive = btTile?.isActive == true

    val isDarkActive = themeState.isDarkMode

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "GLYPH QUICK PANEL",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wifi
                QuickToggleItem(
                    title = "Wi-Fi",
                    iconType = "WIFI",
                    active = wifiActive,
                    accentColor = themeState.accentColor.color,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        wifiTile?.let { viewModel.triggerTileAction(it.id) }
                    }
                )

                // Bluetooth
                QuickToggleItem(
                    title = "Bluetooth",
                    iconType = "BLUETOOTH",
                    active = btActive,
                    accentColor = themeState.accentColor.color,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        btTile?.let { viewModel.triggerTileAction(it.id) }
                    }
                )

                // Airplane
                QuickToggleItem(
                    title = "Airplane",
                    iconType = "AIRPLANE",
                    active = airplaneActive,
                    accentColor = themeState.accentColor.color,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setAirplaneModeActive(!airplaneActive)
                    }
                )

                // Dark mode
                QuickToggleItem(
                    title = "Dark Theme",
                    iconType = "DARKMODE",
                    active = isDarkActive,
                    accentColor = themeState.accentColor.color,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.updateThemeState(
                            themeState.copy(isDarkMode = !isDarkActive)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun QuickToggleItem(
    title: String,
    iconType: String,
    active: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerBg by animateColorAsState(
        targetValue = if (active) accentColor else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(180, easing = LinearOutSlowInEasing)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(containerBg)
                .clickable { onClick() }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            DotMatrixIcon(
                iconType = iconType,
                active = active,
                activeColor = Color.Black,
                inactiveColor = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun DotMatrixChart(
    history: List<Float>,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxPoints = 15

        // Dotted grid lines
        val cols = 6
        val rows = 4
        for (i in 1 until cols) {
            val x = i * (width / cols)
            for (j in 0..rows) {
                val y = j * (height / rows)
                drawCircle(Color.White.copy(alpha = 0.12f), 1.5f, Offset(x, y))
            }
        }
        for (j in 1 until rows) {
            val y = j * (height / rows)
            for (i in 0..cols) {
                val x = i * (width / cols)
                drawCircle(Color.White.copy(alpha = 0.12f), 1.5f, Offset(x, y))
            }
        }

        if (history.isNotEmpty()) {
            val spacing = width / (maxPoints - 1)
            val points = history.takeLast(maxPoints)

            // Dotted lines
            for (index in 0 until points.size - 1) {
                val x1 = index * spacing
                val y1 = height - (points[index] / 100f * height)
                val x2 = (index + 1) * spacing
                val y2 = height - (points[index + 1] / 100f * height)

                drawLine(
                    color = tintColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
            }

            // Highlighting Dots
            for (index in points.indices) {
                val x = index * spacing
                val y = height - (points[index] / 100f * height)
                drawCircle(
                    color = tintColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = tintColor.copy(alpha = 0.25f),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun PerformanceMetricsWidget(
    viewModel: DashboardViewModel,
    themeState: ThemeState,
    modifier: Modifier = Modifier
) {
    val ramUsed by viewModel.ramUsagePercent.collectAsStateWithLifecycle()
    val cpuUsed by viewModel.cpuUsagePercent.collectAsStateWithLifecycle()
    val history by viewModel.cpuHistory.collectAsStateWithLifecycle()
    val isCleaning by viewModel.isRamCleaning.collectAsStateWithLifecycle()

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM ENGINE DIAGNOSTICS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "LIVE TELEMETRY",
                    color = themeState.accentColor.color,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and details Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CPU Progress
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CPU Usage", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.8f))
                        Text("$cpuUsed%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = cpuUsed / 100f,
                        color = themeState.accentColor.color,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }

                // RAM Progress
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RAM Occupancy", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.8f))
                        Text("$ramUsed%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = ramUsed / 100f,
                        color = themeState.accentColor.color,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dotted/matrix chart representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                DotMatrixChart(
                    history = history,
                    tintColor = themeState.accentColor.color,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.purgeMemoryBoooster() },
                enabled = !isCleaning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeState.accentColor.color,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCleaning) "PURGING PERFORMANCE CACHES..." else "PURGE TRANSITION RAM",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GlyphyChatWidget(
    viewModel: DashboardViewModel,
    themeState: ThemeState,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status light dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(themeState.accentColor.color, shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GLYPHY AI ASSISTANT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable view of messages or placeholder greeting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hello! I am Glyphy, your Nothing OS Glyph companion.\nAsk me questions or request quick system diagnostics stats.",
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
                    ) {
                        items(messages) { msg ->
                            val isUser = msg.sender == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isUser) themeState.accentColor.color else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(
                                        topStart = 10.dp,
                                        topEnd = 10.dp,
                                        bottomStart = if (isUser) 10.dp else 2.dp,
                                        bottomEnd = if (isUser) 2.dp else 10.dp
                                    ),
                                    border = if (isUser) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isUser) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input and send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Consult Glyphy...",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank() && !isAiLoading) {
                            viewModel.sendChatMessage(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        }
                    }),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeState.accentColor.color,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                        focusedContainerColor = Color.White.copy(alpha = 0.02f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank() && !isAiLoading) {
                            viewModel.sendChatMessage(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = !isAiLoading,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isAiLoading) Color.White.copy(alpha = 0.1f) else themeState.accentColor.color,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
