package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.ThemeState
import com.example.models.AccentColorType
import com.example.models.BackgroundStyle
import com.example.models.TileShape
import com.example.models.DashboardTile
import com.example.models.TileSize
import com.example.models.TileType
import com.example.viewmodels.DashboardViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current
    var edgeDragOffset by remember { mutableStateOf(0f) }
    var isEdgeDragging by remember { mutableStateOf(false) }
    var activeEdge by remember { mutableStateOf("none") }
    var lastTickSegment by remember { mutableStateOf(0) }
    val drawnPoints = remember { mutableStateListOf<Offset>() }
    var showMacroDialog by remember { mutableStateOf(false) }

    val tiles by viewModel.tiles.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()
    val themeState by viewModel.themeState.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedSwapId by viewModel.selectedTileIdForSwap.collectAsState()
    val customNames by viewModel.customNames.collectAsState()

    // Scenarios and Lab Gestures states
    val scenarios by viewModel.scenarios.collectAsState()
    val isScreenshotTriggered by viewModel.isScreenshotTriggered.collectAsState()
    val gesturePadActive by viewModel.gesturePadActive.collectAsState()

    var isBooting by remember { mutableStateOf(true) }
    var bootFinishTriggered by remember { mutableStateOf(false) }

    val bootAlpha by animateFloatAsState(
        targetValue = if (bootFinishTriggered) 0f else 1f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        finishedListener = {
            if (bootFinishTriggered) {
                isBooting = false
            }
        },
        label = "boot_fade"
    )

    val systemContentScale by animateFloatAsState(
        targetValue = if (bootFinishTriggered) 1.0f else 0.95f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "system_content_scale"
    )

    val systemContentAlpha by animateFloatAsState(
        targetValue = if (bootFinishTriggered) 1.0f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "system_content_alpha"
    )

    // Dialog state for detailed bento adjustments
    var activeControlTile by remember { mutableStateOf<DashboardTile?>(null) }
    
    val isDeepFocusActive = tiles.any { it.type == TileType.FOCUS_TIMER && it.isActive }

    val backgroundModifier = when (themeState.backgroundStyle) {
        BackgroundStyle.SOLID_BLACK -> Modifier.background(Color.Black)
        BackgroundStyle.SOLID_DARK_GREY -> Modifier.background(Color(0xFF141414))
        BackgroundStyle.GRADIENT -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF262626), Color.Black)
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = systemContentScale
                    scaleY = systemContentScale
                    alpha = systemContentAlpha
                }
        ) {
            Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Glyph QS",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "by ashu mehta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = themeState.accentColor.color.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = backgroundModifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Instructions banner
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "info",
                            tint = themeState.accentColor.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "USER MANUAL • OS v3.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Tap cards directly to toggle state, load commands, or open dials.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Click 'Edit Layout' below to swap, resize, and rename any tile.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isDeepFocusActive) {
                        Text("• DEEP FOCUS ENGAGED: Workspace restricted. Tap Focus to cancel.", fontSize = 11.sp, color = themeState.accentColor.color, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Controls & Edit Trigger Room
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Layout width selectors
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4).forEach { cols ->
                        val selected = gridMode == cols
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                .clickable { viewModel.setGridMode(cols) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${cols}xN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selected) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Edit Mode trigger button
                Button(
                    onClick = { viewModel.toggleEditMode() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) themeState.accentColor.color else Color.White.copy(alpha = 0.12f),
                        contentColor = if (isEditMode) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = "edit",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "Done" else "Edit Layout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // LAB HUD CONTROL BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gestures Toggle Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (gesturePadActive) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                        .clickable { viewModel.toggleGesturePad() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (gesturePadActive) Color.Black else themeState.accentColor.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LAB GESTURES: ${if (gesturePadActive) "ON" else "OFF"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (gesturePadActive) Color.Black else Color.White
                        )
                    }
                }

                // Macro dialog toggle button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showMacroDialog = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircleOutline,
                            contentDescription = "play_macro",
                            tint = themeState.accentColor.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SCENARIO MACROS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Edit Mode Active Banner
            if (isEditMode) {
                Surface(
                    color = themeState.accentColor.color.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, themeState.accentColor.color.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "🔧 LAYOUT ENGINE ACTIVE\n" +
                               "1. Click a tile to select. Then click another to SWAP positions\n" +
                               "2. Long press a tile or select below to resize and rename.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Detailed Edit Form if a tile is highlighted in edit mode
            if (isEditMode && selectedSwapId != null) {
                val currentSwapTile = tiles.find { it.id == selectedSwapId }
                if (currentSwapTile != null) {
                    val tileDisplayName = customNames[currentSwapTile.id] ?: currentSwapTile.type.displayName
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, themeState.accentColor.color.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Configure: $tileDisplayName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = themeState.accentColor.color,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Size picker
                            Text("Bento Size Span:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                TileSize.values().forEach { size ->
                                    val isSelected = currentSwapTile.size == size
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                            .clickable { viewModel.resizeTile(currentSwapTile.id, size) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            size.displayName(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom names
                            var textVal by remember(currentSwapTile.id) { mutableStateOf(tileDisplayName) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = textVal,
                                    onValueChange = { 
                                        textVal = it 
                                        viewModel.renameTile(currentSwapTile.id, it)
                                    },
                                    label = { Text("Rename Tile Label", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeState.accentColor.color,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedLabelColor = themeState.accentColor.color
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Theme Options Button
            var expandTheme by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { expandTheme = !expandTheme }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "palette",
                            tint = themeState.accentColor.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (expandTheme) "Mute Theme Panel" else "Nothing OS Custom Theme Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }

            if (expandTheme) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Signature Accent Colors", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentColorType.values().forEach { accent ->
                                val sel = themeState.accentColor == accent
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.color)
                                        .border(
                                            width = if (sel) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.updateThemeState(themeState.copy(accentColor = accent)) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Bento Component Shape", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TileShape.values().forEach { shape ->
                                val sel = themeState.tileShape == shape
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                        .clickable { viewModel.updateThemeState(themeState.copy(tileShape = shape)) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        shape.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Chassis Background Design", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BackgroundStyle.values().forEach { style ->
                                val sel = themeState.backgroundStyle == style
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                        .clickable { viewModel.updateThemeState(themeState.copy(backgroundStyle = style)) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        style.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Active System Theme Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true, false).forEach { isDark ->
                                val sel = themeState.isDarkMode == isDark
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                        .clickable { viewModel.updateThemeState(themeState.copy(isDarkMode = isDark)) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        if (isDark) "Nothing Dark" else "Nothing Light",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // The Bento grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridMode),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = tiles,
                    key = { _, item -> item.id },
                    span = { _, tile ->
                        val boundedSpan = minOf(tile.size.span, gridMode)
                        GridItemSpan(boundedSpan)
                    }
                ) { index, tile ->
                    val isSelectedInEdit = selectedSwapId == tile.id
                    val customLabel = customNames[tile.id] ?: tile.type.displayName

                    var cardVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(20L + index * 25L)
                        cardVisible = true
                    }
                    val cardScale by animateFloatAsState(
                        targetValue = if (cardVisible) 1.0f else 0.9f,
                        animationSpec = tween(
                            durationMillis = 220,
                            easing = FastOutSlowInEasing
                        ),
                        label = "card_scale"
                    )
                    val cardAlpha by animateFloatAsState(
                        targetValue = if (cardVisible) 1.0f else 0.0f,
                        animationSpec = tween(260, easing = LinearOutSlowInEasing),
                        label = "card_alpha"
                    )

                    Box(modifier = Modifier.graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }) {
                        TileItem(
                            tile = tile,
                            customLabel = customLabel,
                            isDeepFocusActive = isDeepFocusActive,
                            themeState = themeState,
                            isSelectedInEdit = isSelectedInEdit,
                            onClick = {
                                if (isEditMode) {
                                    viewModel.handleTileClickInEditMode(tile.id)
                                } else {
                                    // Decide if simple switch or open dial details dialog
                                    if (tile.type.hasDetailedControl()) {
                                        activeControlTile = tile
                                    } else {
                                        viewModel.triggerTileAction(tile.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Configurator popup Dialog
    activeControlTile?.let { tile ->
        val customName = customNames[tile.id] ?: tile.type.displayName
        AlertDialog(
            onDismissRequest = { activeControlTile = null },
            confirmButton = {
                TextButton(onClick = { activeControlTile = null }) {
                    Text("CLOSE SYSTEM DIAL", color = themeState.accentColor.color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(tile.type.icon, contentDescription = null, tint = themeState.accentColor.color, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(customName.uppercase(), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BENTO UTILITY CONTROLS • STATE " + if (tile.isActive) "ENGAGED" else "SUSPENDED",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Conditional layout for deep dial features
                    when (tile.type) {
                        TileType.FOCUS_TIMER -> {
                            FocusTimerConfigurator(viewModel, themeState)
                        }
                        TileType.WIFI_SHARE -> {
                            WifiShareConfigurator(viewModel, themeState)
                        }
                        TileType.DNS -> {
                            DnsConfigurator(viewModel, themeState)
                        }
                        TileType.TERMINAL -> {
                            TerminalConfigurator(viewModel, themeState)
                        }
                        TileType.GLYPH -> {
                            GlyphLightConfigurator(viewModel, themeState)
                        }
                        TileType.DESK_LOCK -> {
                            DeskLockConfigurator(viewModel, themeState)
                        }
                        TileType.CAFFEINE -> {
                            CaffeineKeeperConfigurator(viewModel, themeState)
                        }
                        TileType.SHORTCUTS -> {
                            ShortcutsConfigurator(viewModel, themeState)
                        }
                        TileType.SCREEN_TIMEOUT -> {
                            TimeoutConfigurator(viewModel, tile, themeState)
                        }
                        TileType.COMPASS -> {
                            CompassConfigurator(viewModel, themeState)
                        }
                        TileType.RAM_BOOSTER -> {
                            RamBoosterConfigurator(viewModel, themeState)
                        }
                        TileType.DECIBEL_METER -> {
                            DecibelConfigurator(viewModel, themeState)
                        }
                        TileType.MORSE_FLASHER -> {
                            MorseConfigurator(viewModel, themeState)
                        }
                        TileType.SPEED_TEST -> {
                            SpeedTestConfigurator(viewModel, themeState)
                        }
                        TileType.STOPWATCH -> {
                            StopwatchConfigurator(viewModel, themeState)
                        }
                        TileType.METRONOME -> {
                            MetronomeConfigurator(viewModel, themeState)
                        }
                        TileType.SOUNDBOARD -> {
                            SoundboardConfigurator(viewModel, themeState)
                        }
                        TileType.PIXEL_ART -> {
                            PixelArtConfigurator(viewModel, themeState)
                        }
                        TileType.REACTION_TEST -> {
                            ReactionTestConfigurator(viewModel, themeState)
                        }
                        TileType.DICE_COIN -> {
                            DiceCoinConfigurator(viewModel, themeState)
                        }
                        TileType.CPU_TEMP -> {
                            CpuTempConfigurator(viewModel, themeState)
                        }
                        TileType.PASSWORD_GEN -> {
                            PasswordConfigurator(viewModel, themeState)
                        }
                        TileType.WORLD_CLOCK -> {
                            WorldClocksConfigurator(viewModel, themeState)
                        }
                        TileType.QUICK_NOTES -> {
                            QuickNotesConfigurator(viewModel, themeState)
                        }
                        TileType.MACRO_EDITOR -> {
                            MacroEditorConfigurator(viewModel, themeState)
                        }
                        else -> {
                            Text("No advanced config properties for this Bento component.", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            },
            containerColor = Color(0xFF161616),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        )
    }

    // 1. GESTURE BLACKBOARD OVERLAY
    if (gesturePadActive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            drawnPoints.clear()
                            drawnPoints.add(offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            drawnPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (drawnPoints.size > 8) {
                                val first = drawnPoints.first()
                                val last = drawnPoints.last()
                                val xs = drawnPoints.map { it.x }
                                val ys = drawnPoints.map { it.y }
                                val minX = xs.min()
                                val maxX = xs.max()
                                val minY = ys.min()
                                val maxY = ys.max()
                                val wWidth = maxX - minX
                                val hHeight = maxY - minY
                                val closedDist = Math.hypot((last.x - first.x).toDouble(), (last.y - first.y).toDouble())

                                if (closedDist < 90f && wWidth > 90f && hHeight > 90f) {
                                    viewModel.triggerGestureAction("Circle O")
                                } else if (wWidth < 70f && hHeight > 150f) {
                                    viewModel.triggerGestureAction("Line I")
                                }
                            }
                            drawnPoints.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = 30.dp.toPx()
                for (x in 0..(size.width / cellSize).toInt()) {
                    for (y in 0..(size.height / cellSize).toInt()) {
                        drawCircle(
                            color = themeState.accentColor.color.copy(alpha = 0.08f),
                            radius = 1.dp.toPx(),
                            center = Offset(x * cellSize, y * cellSize)
                        )
                    }
                }

                if (drawnPoints.size > 1) {
                    for (i in 0 until drawnPoints.size - 1) {
                        val alphaNorm = i.toFloat() / drawnPoints.size
                        drawLine(
                            color = themeState.accentColor.color.copy(alpha = alphaNorm),
                            start = drawnPoints[i],
                            end = drawnPoints[i + 1],
                            strokeWidth = 5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = alphaNorm),
                            radius = 2.dp.toPx(),
                            center = drawnPoints[i]
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "DOT-MATRIX CHALKBOARD",
                    fontWeight = FontWeight.Black,
                    color = themeState.accentColor.color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Draw Circle 'O' for Torch • Vertical line down 'I' for memory dump • Two-finger touch for capture screen",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.toggleGesturePad() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                ) {
                    Text("EXIT GESTURES", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    // 2. DETECTOR OVERLAYS FOR PREDICTIVE BACK BEZEL SWIPES
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(24.dp)
            .align(Alignment.CenterStart)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isEdgeDragging = true
                        activeEdge = "left"
                        edgeDragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        edgeDragOffset += dragAmount.x
                        val currentSegment = (edgeDragOffset / 40f).toInt()
                        if (currentSegment != lastTickSegment) {
                            viewModel.playHapticVibration("TICK")
                            lastTickSegment = currentSegment
                        }
                    },
                    onDragEnd = {
                        if (edgeDragOffset > 180f) {
                            viewModel.playHapticVibration("DOUBLE_TAP")
                            if (activeControlTile != null) {
                                activeControlTile = null
                            } else if (isEditMode) {
                                viewModel.toggleEditMode()
                            } else if (showMacroDialog) {
                                showMacroDialog = false
                            } else if (gesturePadActive) {
                                viewModel.toggleGesturePad()
                            }
                            Toast.makeText(context, "Predictive Back Triggered", Toast.LENGTH_SHORT).show()
                        }
                        isEdgeDragging = false
                        edgeDragOffset = 0f
                    }
                )
            }
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(24.dp)
            .align(Alignment.CenterEnd)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isEdgeDragging = true
                        activeEdge = "right"
                        edgeDragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        edgeDragOffset -= dragAmount.x
                        val currentSegment = (edgeDragOffset / 40f).toInt()
                        if (currentSegment != lastTickSegment) {
                            viewModel.playHapticVibration("TICK")
                            lastTickSegment = currentSegment
                        }
                    },
                    onDragEnd = {
                        if (edgeDragOffset > 180f) {
                            viewModel.playHapticVibration("DOUBLE_TAP")
                            if (activeControlTile != null) {
                                activeControlTile = null
                            } else if (isEditMode) {
                                viewModel.toggleEditMode()
                            } else if (showMacroDialog) {
                                showMacroDialog = false
                            } else if (gesturePadActive) {
                                viewModel.toggleGesturePad()
                            }
                            Toast.makeText(context, "Predictive Back Triggered", Toast.LENGTH_SHORT).show()
                        }
                        isEdgeDragging = false
                        edgeDragOffset = 0f
                    }
                )
            }
    )

    // Render Edge Drag Gauge UI
    if (isEdgeDragging && edgeDragOffset > 10f) {
        val progressNorm = (edgeDragOffset / 200f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f * progressNorm))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val drawLeft = activeEdge == "left"
                val arcX = if (drawLeft) 0f else size.width
                val arcY = size.height / 2
                val maxArcRadius = 140.dp.toPx()
                val curRadius = maxArcRadius * progressNorm

                drawCircle(
                    color = themeState.accentColor.color.copy(alpha = 0.5f * progressNorm),
                    radius = curRadius,
                    center = Offset(arcX, arcY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(8f, 10f), 0f
                        )
                    )
                )

                drawCircle(
                    color = themeState.accentColor.color.copy(alpha = 0.2f * progressNorm),
                    radius = curRadius + 40f,
                    center = Offset(arcX, arcY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(6f, 14f), 0f
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .align(if (activeEdge == "left") Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = if (activeEdge == "left") Alignment.Start else Alignment.End
            ) {
                Text(
                    text = "SWIPE TENSION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeState.accentColor.color
                )
                Text(
                    text = "DISMISS FORCE: ${(progressNorm * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressNorm,
                    modifier = Modifier.width(100.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = themeState.accentColor.color,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }

    // 3. SHUTTER SCREENSHOT CAPTURE FLASH
    if (isScreenshotTriggered) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }

    // 4. INDEPENDENT MACRO EDITOR SCENARIO DIALOG
    if (showMacroDialog) {
        AlertDialog(
            onDismissRequest = { showMacroDialog = false },
            confirmButton = {
                TextButton(onClick = { showMacroDialog = false }) {
                    Text("CLOSE LAB", color = themeState.accentColor.color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SettingsInputComponent, contentDescription = null, tint = themeState.accentColor.color, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCENARIOS LAB", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                }
            },
            text = {
                MacroEditorConfigurator(viewModel, themeState)
            },
            containerColor = Color(0xFF141414),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        )
    }

    if (isDeepFocusActive) {
        val timeString = tiles.find { it.type == TileType.FOCUS_TIMER }?.displayValue ?: "25:00"
        FocalLockoutBodyguardOverlay(viewModel, themeState, timeString)
    }
    }

    if (isBooting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bootAlpha }
                .clickable(enabled = true, onClick = {})
        ) {
            BootScreen(themeState) {
                bootFinishTriggered = true
            }
        }
    }
}
}

// Check if a tile type should open a custom dialog instead of standard trigger toggling
fun TileType.hasDetailedControl(): Boolean {
    return this == TileType.FOCUS_TIMER ||
           this == TileType.WIFI_SHARE ||
           this == TileType.DNS ||
           this == TileType.TERMINAL ||
           this == TileType.GLYPH ||
           this == TileType.DESK_LOCK ||
           this == TileType.CAFFEINE ||
           this == TileType.SHORTCUTS ||
           this == TileType.SCREEN_TIMEOUT ||
           this == TileType.COMPASS ||
           this == TileType.RAM_BOOSTER ||
           this == TileType.DECIBEL_METER ||
           this == TileType.MORSE_FLASHER ||
           this == TileType.SPEED_TEST ||
           this == TileType.STOPWATCH ||
           this == TileType.METRONOME ||
           this == TileType.SOUNDBOARD ||
           this == TileType.PIXEL_ART ||
           this == TileType.REACTION_TEST ||
           this == TileType.DICE_COIN ||
           this == TileType.CPU_TEMP ||
           this == TileType.PASSWORD_GEN ||
           this == TileType.WORLD_CLOCK ||
           this == TileType.QUICK_NOTES ||
           this == TileType.MACRO_EDITOR
}

@Composable
fun MorphingGlyphIcon(
    tileType: TileType,
    isActive: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isActive, label = "glyph_morph")
    
    val morphProgress by transition.animateFloat(
        transitionSpec = { tween(240, easing = FastOutSlowInEasing) },
        label = "morph"
    ) { active ->
        if (active) 1.0f else 0.0f
    }
    
    val pulseScale by rememberInfiniteTransition(label = "pulse_scale").animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.14f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.graphicsLayer {
        scaleX = if (isActive) pulseScale else 1.0f
        scaleY = if (isActive) pulseScale else 1.0f
    }) {
        val sizePx = size.width
        val center = Offset(sizePx / 2, sizePx / 2)
        val strokeWidth = 2.dp.toPx()

        when (tileType) {
            TileType.THEATER, TileType.FOCUS_TIMER, TileType.CAFFEINE -> {
                val radius = (sizePx / 2) * (0.4f + 0.3f * morphProgress)
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
                if (morphProgress < 0.9f) {
                    val angle = 45f * (1f - morphProgress)
                    rotate(angle, center) {
                        drawLine(
                            color = tint,
                            start = Offset(center.x - radius, center.y),
                            end = Offset(center.x + radius, center.y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
            TileType.WIFI, TileType.BLUETOOTH, TileType.WIFI_SHARE -> {
                val lobes = 3
                for (i in 1..lobes) {
                    val currentRadius = (sizePx / (lobes * 2)) * i * (0.6f + 0.4f * morphProgress)
                    drawCircle(
                        color = tint.copy(alpha = if (isActive) 1f - (i * 0.2f) else 0.4f),
                        radius = currentRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            pathEffect = if (!isActive) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null
                        )
                    )
                }
            }
            else -> {
                if (isActive) {
                    val padding = 2.dp.toPx()
                    val len = 4.dp.toPx()
                    // top left
                    drawLine(tint, Offset(padding, padding), Offset(padding + len, padding), strokeWidth)
                    drawLine(tint, Offset(padding, padding), Offset(padding, padding + len), strokeWidth)
                    // top right
                    drawLine(tint, Offset(sizePx - padding, padding), Offset(sizePx - padding - len, padding), strokeWidth)
                    drawLine(tint, Offset(sizePx - padding, padding), Offset(sizePx - padding, padding + len), strokeWidth)
                    // bottom left
                    drawLine(tint, Offset(padding, sizePx - padding), Offset(padding + len, sizePx - padding), strokeWidth)
                    drawLine(tint, Offset(padding, sizePx - padding), Offset(padding, sizePx - padding - len), strokeWidth)
                    // bottom right
                    drawLine(tint, Offset(sizePx - padding, sizePx - padding), Offset(sizePx - padding - len, sizePx - padding), strokeWidth)
                    drawLine(tint, Offset(sizePx - padding, sizePx - padding), Offset(sizePx - padding, sizePx - padding - len), strokeWidth)

                    drawCircle(tint, 1.51f.dp.toPx(), Offset(center.x - 6.dp.toPx(), center.y - 6.dp.toPx()))
                    drawCircle(tint, 1.51f.dp.toPx(), Offset(center.x + 6.dp.toPx(), center.y + 6.dp.toPx()))
                }
                
                drawCircle(
                    color = tint.copy(alpha = if (isActive) 0.15f else 0.05f),
                    radius = sizePx / 2.3f,
                    center = center
                )
                drawCircle(
                    color = tint,
                    radius = 2.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

fun TileSize.displayName(): String {
    return when (this) {
        TileSize.SMALL -> "1x1 (Compact)"
        TileSize.MEDIUM_WIDE -> "2x1 (Wide)"
        TileSize.LARGE_SQUARE -> "2x2 (Bento)"
        TileSize.WIDE -> "4x1 (Full)"
    }
}

// ==========================================
// BENTO RENDER ELEMENTS
// ==========================================

@Composable
fun TileItem(
    tile: DashboardTile,
    customLabel: String,
    isDeepFocusActive: Boolean,
    themeState: ThemeState,
    isSelectedInEdit: Boolean,
    onClick: () -> Unit
) {
    val isActive = tile.isActive
    val baseHeight = 85.dp
    val spacing = 10.dp
    val height = if (tile.size.isTall) (baseHeight * 2) + spacing else baseHeight
    
    val dimmedAlpha = if (isDeepFocusActive && tile.type != TileType.FOCUS_TIMER) 0.35f else 1.0f
    
    val containerColor by animateColorAsState(
        targetValue = if (isActive) themeState.accentColor.color else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
    )
    
    val shape = when (themeState.tileShape) {
        TileShape.ROUNDED -> RoundedCornerShape(16.dp)
        TileShape.SQUIRCLE -> RoundedCornerShape(26.dp)
        TileShape.SQUARE -> RoundedCornerShape(2.dp)
    }

    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else Color.White,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
    )

    val editBorder = if (isSelectedInEdit) {
        Modifier.border(2.dp, themeState.accentColor.color, shape)
    } else Modifier

    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor.copy(alpha = containerColor.alpha * dimmedAlpha))
            .clickable(
                enabled = !isDeepFocusActive || tile.type == TileType.FOCUS_TIMER,
                onClick = onClick
            )
            .then(editBorder)
            .padding(12.dp)
    ) {
        val appliedContentColor = contentColor.copy(alpha = contentColor.alpha * dimmedAlpha)
        
        when (tile.type) {
            TileType.USAGE_STATS -> UsageStatsContent(tile, appliedContentColor)
            TileType.TERMINAL -> TerminalContent(tile, appliedContentColor)
            TileType.BATTERY, TileType.STORAGE -> LargeStatContent(tile, customLabel, appliedContentColor)
            TileType.FOCUS_TIMER -> FocusTimerContent(tile, customLabel, appliedContentColor)
            else -> DefaultTileContent(tile, customLabel, appliedContentColor)
        }
    }
}

@Composable
fun DefaultTileContent(tile: DashboardTile, label: String, tint: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (tile.size == TileSize.SMALL) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MorphingGlyphIcon(
                tileType = tile.type,
                isActive = tile.isActive,
                tint = tint,
                modifier = Modifier.size(if (tile.size == TileSize.SMALL) 28.dp else 22.dp)
            )
            if (tile.size != TileSize.SMALL) {
                Text(
                    text = label,
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
        if (tile.displayValue != null && tile.size != TileSize.SMALL) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tile.displayValue!!,
                color = tint.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LargeStatContent(tile: DashboardTile, label: String, tint: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MorphingGlyphIcon(tile.type, tile.isActive, tint, Modifier.size(20.dp))
            Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            text = tile.displayValue ?: "...",
            color = tint,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun UsageStatsContent(tile: DashboardTile, tint: Color) {
    val viewModel: DashboardViewModel = viewModel()
    val rawUsage by viewModel.useCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(tile.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text("USAGE CHARTS", color = tint, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.weight(1f))
        
        // Render 5 interactive bars reflecting actual tile tap weights!
        val sortedWeights = rawUsage.values.sortedDescending().take(5)
        val maxVal = if (sortedWeights.firstOrNull() == 0 || sortedWeights.isEmpty()) 5f else sortedWeights.first().toFloat()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Take 5 random/existing weights to render beautiful bars
            listOf(0.8f, 0.4f, 0.9f, 0.5f, 0.6f).forEachIndexed { idx, mockFraction ->
                val realFrac = if (idx < sortedWeights.size) sortedWeights[idx].toFloat() / maxVal else mockFraction
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .fillMaxHeight(coerceFraction(realFrac))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(tint)
                )
            }
        }
    }
}

private fun coerceFraction(value: Float): Float {
    return value.coerceIn(0.15f, 1.0f)
}

@Composable
fun TerminalContent(tile: DashboardTile, tint: Color) {
    val viewModel: DashboardViewModel = viewModel()
    val logs by viewModel.terminalLogs.collectAsState()
    val lastLog = logs.lastOrNull() ?: "user@nothing:~$ ready"

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(tile.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text("GLYPH_SHELL", color = tint, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = lastLog,
            color = tint.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2
        )
    }
}

@Composable
fun FocusTimerContent(tile: DashboardTile, label: String, tint: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(tile.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tile.displayValue ?: "25:00",
                color = tint,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = if (tile.isActive) "TAP TO LOCK" else "TAP TO START",
            color = tint.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ==========================================
// COMPONENT SYSTEM CONFIURATORS
// ==========================================

@Composable
fun FocusTimerConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val tiles by viewModel.tiles.collectAsState()
    val focusTile = tiles.find { it.type == TileType.FOCUS_TIMER }
    val isActive = focusTile?.isActive == true
    val focusMinutes by viewModel.focusMinutes.collectAsState()
    val whitelist by viewModel.whitelistedApps.collectAsState()

    Text("Configure your Deep Focus parameters. When active, Zen Bodyguard shields your phone.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))

    // Interactive Timer Progress
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Timer State:", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(if (isActive) "BODYGUARD RUNNING" else "STANDBY READY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) Color.Red else themeState.accentColor.color)
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (!isActive) {
        Text("Select Timeout Duration: ${focusMinutes} Min (${String.format("%.1f", focusMinutes / 60.0)} hrs)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = focusMinutes.toFloat(),
            onValueChange = { viewModel.setFocusMinutes(it.toInt()) },
            valueRange = 1f..240f,
            colors = SliderDefaults.colors(
                thumbColor = themeState.accentColor.color,
                activeTrackColor = themeState.accentColor.color,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text("Permitted Ecosystem Whitelist:", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))

        val whitelistRow1 = listOf("Phone", "Messages", "Settings", "Maps", "Clock")
        val whitelistRow2 = listOf("Spotify", "Calculator", "WhatsApp", "YouTube", "Chrome")

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                whitelistRow1.forEach { app ->
                    val isWhitelisted = whitelist.contains(app)
                    val isEssential = app in listOf("Phone", "Messages", "Settings")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isWhitelisted) themeState.accentColor.color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isWhitelisted) themeState.accentColor.color else Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .clickable { if (!isEssential) viewModel.toggleAppWhitelist(app) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = (if (isWhitelisted) "✓ " else "+ ") + app,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWhitelisted) themeState.accentColor.color else Color.White
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                whitelistRow2.forEach { app ->
                    val isWhitelisted = whitelist.contains(app)
                    val isEssential = app in listOf("Phone", "Messages", "Settings")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isWhitelisted) themeState.accentColor.color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isWhitelisted) themeState.accentColor.color else Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .clickable { if (!isEssential) viewModel.toggleAppWhitelist(app) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = (if (isWhitelisted) "✓ " else "+ ") + app,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWhitelisted) themeState.accentColor.color else Color.White
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    Button(
        onClick = { viewModel.triggerTileAction(focusTile?.id ?: "") },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color.Red else themeState.accentColor.color,
            contentColor = Color.Black
        )
    ) {
        Text(if (isActive) "DISMISSED DEEP BODYGUARD" else "ARM DEEP BODYGUARD LOCKDOWN", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
fun WifiShareConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val ssid by viewModel.wifiSsid.collectAsState()
    val pass by viewModel.wifiPass.collectAsState()

    var editingSsid by remember { mutableStateOf(ssid) }
    var editingPass by remember { mutableStateOf(pass) }

    Text("Generate dynamic dot-matrix Wi-Fi join markers for guests.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        value = editingSsid,
        onValueChange = { 
            editingSsid = it
            viewModel.setWifiConfiguration(it, editingPass)
        },
        label = { Text("Network SSID", fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = editingPass,
        onValueChange = { 
            editingPass = it
            viewModel.setWifiConfiguration(editingSsid, it)
        },
        label = { Text("Passcode", fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))
    Text("QR Matrix Simulator:", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(8.dp))

    // Draw an ultra gorgeous Nothing elements QR simulated matrix code using canvas!
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Color.White)
                .padding(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rows = 12
                val cols = 12
                val cellW = size.width / cols
                val cellH = size.height / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        // Seed standard QR looking corner frames
                        val isCorner = (r < 3 && c < 3) || (r < 3 && c >= cols - 3) || (r >= rows - 3 && c < 3)
                        val pseudoRandom = ((r * 13 + c * 7 + (ssid.length + pass.length)) % 3) == 0
                        if (isCorner || pseudoRandom) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = androidx.compose.ui.geometry.Size(cellW - 2, cellH - 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DnsConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val currentDns by viewModel.currentDns.collectAsState()

    var customDnsInput by remember { mutableStateOf("") }

    Text("Force routing systems through private secure DNS servers.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))

    val presets = listOf("Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "AdGuard (76.76.19.19)")

    presets.forEach { d ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setPrivateDns(d) }
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = currentDns == d,
                onClick = { viewModel.setPrivateDns(d) },
                colors = RadioButtonDefaults.colors(selectedColor = themeState.accentColor.color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(d, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = customDnsInput,
            onValueChange = { customDnsInput = it },
            label = { Text("Custom IP Address", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Button(
            onClick = {
                if (customDnsInput.isNotEmpty()) {
                    viewModel.setPrivateDns(customDnsInput)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
        ) {
            Text("SET", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun TerminalConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val logs by viewModel.terminalLogs.collectAsState()
    var inputStr by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Text("Frictionless utility shell. Query indicators, test hardware LEDs.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            logs.forEach { log ->
                Text(
                    text = log,
                    color = if (log.startsWith(">")) themeState.accentColor.color else Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            LaunchedEffect(logs.size) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputStr,
            onValueChange = { inputStr = it },
            placeholder = { Text("Enter command...", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (inputStr.isNotEmpty()) {
                    viewModel.executeTerminalCommand(inputStr)
                    inputStr = ""
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeState.accentColor.color,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Button(
            onClick = {
                if (inputStr.isNotEmpty()) {
                    viewModel.executeTerminalCommand(inputStr)
                    inputStr = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
        ) {
            Text("RUN", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlyphLightConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val intensity by viewModel.glyphIntensity.collectAsState()
    val isGlyphBlinking by viewModel.isGlyphBlinking.collectAsState()

    Text("Orchestrate simulated Nothing Glyph rear hardware matrix LED panels.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))

    Text("Glyph Back LEDs Brightness ($intensity%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Slider(
        value = intensity.toFloat(),
        onValueChange = { viewModel.updateGlyphIntensity(it.toInt()) },
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(
            thumbColor = themeState.accentColor.color,
            activeTrackColor = themeState.accentColor.color
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text("Rear LED Simulation Array:", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(6.dp))

    // Render an actual glowing LED simulated pattern inspired by the rear glass!
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Pulsing logic
            val alphaGlow = if (isGlyphBlinking) 0.9f else 0.4f
            val pathColor = themeState.accentColor.color.copy(alpha = alphaGlow)
            
            // Draw Glyph camera ring segment
            drawCircle(
                color = pathColor,
                radius = 35f,
                center = Offset(center.x - 60f, center.y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
            )

            // Draw Glyph central slope segment
            drawLine(
                color = pathColor,
                start = Offset(center.x, center.y - 30f),
                end = Offset(center.x, center.y + 35f),
                strokeWidth = 6f
            )

            // Draw Glyph slash segment
            drawLine(
                color = pathColor,
                start = Offset(center.x + 60f, center.y - 35f),
                end = Offset(center.x + 60f, center.y + 10f),
                strokeWidth = 6f
            )

            // Draw indicator dot
            drawCircle(
                color = pathColor,
                radius = 8f,
                center = Offset(center.x + 60f, center.y + 30f)
            )
        }
    }
}

@Composable
fun DeskLockConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val distance by viewModel.deskLockDistance.collectAsState()
    val isConnected by viewModel.isDeskLockConnected.collectAsState()

    Text("Proximity workstation locker binds your Android terminal session to computer distance.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Proximity Signal Status:", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(
            text = if (isConnected) "CONNECTED" else "DISCONNECTED (OFFLINE)",
            color = if (isConnected) Color.Green else Color.Red,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text("Proximity Threshold (${String.format("%.1f", distance)}m)", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    Slider(
        value = distance,
        onValueChange = { viewModel.setDeskLockDistance(it) },
        valueRange = 1f..10f,
        colors = SliderDefaults.colors(
            thumbColor = themeState.accentColor.color,
            activeTrackColor = themeState.accentColor.color
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = { viewModel.setDeskLockConnected(!isConnected) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isConnected) Color.Red else themeState.accentColor.color,
            contentColor = Color.Black
        )
    ) {
        Text(if (isConnected) "SIMULATE SIGNAL LOSS" else "SIMULATE WORKSPACE RECONNECT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun CaffeineKeeperConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val caffeineOption by viewModel.caffeineOption.collectAsState()
    val timeLeft by viewModel.caffeineTimeLeft.collectAsState()

    Text("Forces screen wake locks so your device never dims during operations.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))

    Text("Interval Wake Timeout:", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(6.dp))

    val options = listOf("5 Min", "15 Min", "30 Min", "Infinite")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { opt ->
            val sel = caffeineOption == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                    .clickable { viewModel.setCaffeineOption(opt) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sel) Color.Black else Color.White
                )
            }
        }
    }

    timeLeft?.let {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Power Level Hold Lock:", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(it, color = themeState.accentColor.color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ShortcutsConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    var mappingResult by remember { mutableStateOf("Ready mapping.") }

    Text("Map physical actions or button triggers directly to task launchers.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))

    val shortcuts = listOf(
        "Double-Click Power -> Camera trigger overlay",
        "Pinch Home Screen -> Clean Memory Cache mapper",
        "Swipe Back -> Run Private Terminal instances",
        "LED Timer Hold -> Reboot Payload triggers"
    )

    shortcuts.forEach { short ->
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clickable { mappingResult = "Trigger success: $short mapped successfully." },
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Black)
        ) {
            Text(
                text = short,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(10.dp),
                color = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(mappingResult, fontSize = 11.sp, color = themeState.accentColor.color, fontWeight = FontWeight.Bold)
}

@Composable
fun TimeoutConfigurator(viewModel: DashboardViewModel, tile: DashboardTile, themeState: ThemeState) {
    val timeouts = listOf("15s", "30s", "1m", "5m", "10m")
    
    Text("Select standard hardware idle delay timers.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        timeouts.forEach { timeout ->
            val sel = tile.displayValue == timeout
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                    .clickable { 
                        tile.displayValue = timeout
                        viewModel.triggerTileAction(tile.id)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeout,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sel) Color.Black else Color.White
                )
            }
        }
    }
}

// ==========================================
// EXPANSION BENCHMARK & UTILITY PLUGINS CARDS
// ==========================================

@Composable
fun CompassConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val bearing by viewModel.compassBearing.collectAsState()
    Text("Rear-facing magnetic sensor simulation. Aligns systems with precise spatial headings.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
            // Draw needle pointing to bearing!
            val angleRad = Math.toRadians((bearing - 90f).toDouble())
            val tipX = center.x + radius * Math.cos(angleRad).toFloat()
            val tipY = center.y + radius * Math.sin(angleRad).toFloat()
            
            // Draw red custom pointer
            drawLine(
                color = themeState.accentColor.color,
                start = center,
                end = Offset(tipX, tipY),
                strokeWidth = 6f
            )
            // Center hub
            drawCircle(color = Color.White, radius = 8f)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text("Heading Alignment: ${String.format("%.1f°", bearing)} Reference North", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

@Composable
fun RamBoosterConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val ramUsed by viewModel.ramUsagePercent.collectAsState()
    val isCleaning by viewModel.isRamCleaning.collectAsState()
    
    Text("Cleans transient background app processes to optimize instruction sets and free up cache blocks.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Active RAM Occupancy:", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("$ramUsed% of 12 GB", color = themeState.accentColor.color, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = ramUsed / 100f,
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
        color = themeState.accentColor.color,
        trackColor = Color.White.copy(alpha = 0.1f)
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { viewModel.purgeMemoryBoooster() },
        enabled = !isCleaning,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
    ) {
        Text(if (isCleaning) "PURGING TRANSITION CACHES..." else "PURGE RAM CACHES", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun DecibelConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val db by viewModel.decibelValue.collectAsState()
    Text("Real-time acoustic analysis. Measures localized atmospheric pressure fluctuations.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Noise Level: ", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("$db dB SPL", color = themeState.accentColor.color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
    }
    Spacer(modifier = Modifier.height(10.dp))
    
    // Rhythmic indicator peaks
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val count = 16
        for (i in 0 until count) {
            val hFraction = ((db * (i + 1) * 31) % 40) / 40f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(hFraction.coerceIn(0.1f, 1.0f))
                    .background(themeState.accentColor.color.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
fun MorseConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val morseText by viewModel.morseText.collectAsState()
    val isFlashing by viewModel.isMorseFlashing.collectAsState()
    var input by remember { mutableStateOf(morseText) }
    
    Text("Translate standard characters into flashlight pulses automatically with sound synthesize ticks.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    OutlinedTextField(
        value = input,
        onValueChange = { 
            input = it
            viewModel.setMorseMessage(it)
        },
        label = { Text("Morse Payload Input", fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = themeState.accentColor.color,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
        )
    )
    Spacer(modifier = Modifier.height(12.dp))
    
    Button(
        onClick = { viewModel.triggerMorseFlasher() },
        enabled = input.isNotEmpty() && !isFlashing,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
    ) {
        Text(if (isFlashing) "TRANSMITTING PULSES..." else "TRANSMIT MORSE SEQUENCE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SpeedTestConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val mbps by viewModel.speedBenchmarkMbps.collectAsState()
    val isTesting by viewModel.isBenchmarking.collectAsState()
    
    Text("Symmetric internet throughput speed benchmark test.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(10.dp))
    
    Box(
        modifier = Modifier.fillMaxWidth().height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            
            // Draw dial arch
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
            )
            
            val maxSpeed = 300f
            val sweptAngle = (mbps.coerceAtMost(maxSpeed) / maxSpeed) * 270f
            
            // Draw active swept arc
            drawArc(
                color = themeState.accentColor.color,
                startAngle = 135f,
                sweepAngle = sweptAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
            )
            
            // Draw rotating needle!
            val angleRad = Math.toRadians((135f + sweptAngle - 90f).toDouble())
            val pointerX = center.x + (radius - 12f) * Math.cos(angleRad).toFloat()
            val pointerY = center.y + (radius - 12f) * Math.sin(angleRad).toFloat()
            drawLine(
                color = themeState.accentColor.color,
                start = center,
                end = Offset(pointerX, pointerY),
                strokeWidth = 4f
            )
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Benchmark Result:", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(String.format("%.1f Mbps", mbps), color = themeState.accentColor.color, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = { viewModel.runSpeedBenchmark() },
        enabled = !isTesting,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
    ) {
        Text(if (isTesting) "RUNNING TRACE BENCH..." else "START THROUGHPUT BENCHMARK", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun StopwatchConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val display by viewModel.stopwatchDisplay.collectAsState()
    val isRunning by viewModel.isStopwatchRunning.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()
    
    Text("High accuracy split stopwatch logs parameters up to millisecond precision.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Box(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(display, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
    }
    
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { viewModel.toggleStopwatch() },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(if (isRunning) "PAUSE" else "START", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Button(
            onClick = { viewModel.lapStopwatch() },
            enabled = isRunning,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("LAP REC", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Button(
            onClick = { viewModel.resetStopwatch() },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("CLEAR", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
    
    if (laps.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("Recorded Split Schedulers:", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier.fillMaxWidth().height(80.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            laps.forEach { lap ->
                Text(lap, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun MetronomeConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val bpm by viewModel.metronomeBpm.collectAsState()
    val isPlaying by viewModel.isMetronomePlaying.collectAsState()
    
    Text("Rhythmic pulse system clocks. Sync devices, flash retro dot matrix strobes in tempo.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Text("Beats Per Minute ($bpm BPM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Slider(
        value = bpm.toFloat(),
        onValueChange = { viewModel.setMetronomeBpm(it.toInt()) },
        valueRange = 40f..240f,
        colors = SliderDefaults.colors(thumbColor = themeState.accentColor.color, activeTrackColor = themeState.accentColor.color)
    )
    
    Spacer(modifier = Modifier.height(10.dp))
    Button(
        onClick = { viewModel.toggleMetronome() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
    ) {
        Text(if (isPlaying) "STOP METRONOME" else "START AUDIO TICK METRONOME", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
fun SoundboardConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val x by viewModel.synthPitchX.collectAsState()
    val y by viewModel.synthFreqY.collectAsState()
    val wave by viewModel.synthWaveform.collectAsState()
    
    Text("O-Synth soundboard. Drag finger across surface area to synthesise pitch harmonics.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Sine", "Square", "Beep").forEach { opt ->
            val sel = wave == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                    .clickable { viewModel.setSynthWaveform(opt) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.Black else Color.White)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text("Interactive Synthesizer Pad", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val finalX = (change.position.x / size.width).coerceIn(0f, 1f)
                    val finalY = (change.position.y / size.height).coerceIn(0f, 1f)
                    viewModel.updateSynthCoords(finalX, finalY)
                }
            }
            .clickable { viewModel.updateSynthCoords(0.4f, 0.5f) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = themeState.accentColor.color, radius = 6f, center = Offset(x * size.width, y * size.height))
            drawCircle(color = themeState.accentColor.color.copy(alpha = 0.2f), radius = 22f, center = Offset(x * size.width, y * size.height))
            // Crosshairs
            drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(0f, y * size.height), end = Offset(size.width, y * size.height))
            drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(x * size.width, 0f), end = Offset(x * size.width, size.height))
        }
        Text("TOUCH / DRAG MATRIX AREA", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PixelArtConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val grid by viewModel.pixelGrid.collectAsState()
    Text("Pixel studio custom graphics compiler. Map LED arrays dynamically.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until 8) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 8) {
                        val idx = r * 8 + c
                        val active = grid[idx]
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                .clickable { viewModel.togglePixelIndex(idx) }
                        )
                    }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = { viewModel.clearPixelGrid() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White)
    ) {
        Text("CLEAR LED MATRIX GRAFT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
fun ReactionTestConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val status by viewModel.gameStatus.collectAsState()
    val latency by viewModel.gameDelayResult.collectAsState()
    
    Text("Test human mechanical reflexes delay indexes.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    val screenColor = when (status) {
        "WAIT" -> Color(0xFFD5A000) // Amber
        "TAP_NOW" -> Color(0xFF00D550) // Green
        "READY" -> Color(0xFFFF2E2E) // Red
        else -> Color.Black
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(screenColor)
            .clickable { 
                if (status == "TAP_NOW" || status == "WAIT") {
                    viewModel.tapReactionTrigger()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val labelText = when (status) {
            "READY" -> "TAP SYSTEM TO PREPARE"
            "WAIT" -> "STANDBY... WAIT FOR GREEN!"
            "TAP_NOW" -> "TAP INSTANTLY!"
            "SCORED" -> "FINAL LATENCY SCORE"
            else -> ""
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(labelText, color = if (status == "READY" || status == "SCORED") Color.White else Color.Black, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            if (latency != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("$latency ms", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = { viewModel.resetReactionGame() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
    ) {
        Text("RE-ARM REFLEX TARGET", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun DiceCoinConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val diceVal by viewModel.diceValue.collectAsState()
    val coinState by viewModel.coinState.collectAsState()
    val isRolling by viewModel.isRollingResult.collectAsState()
    
    Text("RNG system outcomes dice and coin flip matrix solver.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        // Render beautiful Dice item
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("6-SIDED DICE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, themeState.accentColor.color, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("$diceVal", fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
            }
        }
        
        // Render Coin item
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("COIN FLIPPER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, themeState.accentColor.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(coinState.substring(0, 1), fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { viewModel.rollDiceAndCoin() },
        enabled = !isRolling,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
    ) {
        Text(if (isRolling) "SHAKING DICE TUMBLERS..." else "ROLL & SPIN MATRIX OUTCOME", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CpuTempConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val temp by viewModel.cpuTempUnit.collectAsState()
    Text("Internal silicon thermal threshold logging.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Silicon Processor Temp:", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("$temp °C", color = themeState.accentColor.color, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
    }
    Spacer(modifier = Modifier.height(10.dp))
    
    // Draw classic dot graph showing CPU load thermals
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / 14
            for (i in 0 until 14) {
                val ptH = size.height - ((temp - 10 + (i * 7) % 13) / 50f) * size.height
                drawCircle(
                    color = themeState.accentColor.color,
                    radius = 3f,
                    center = Offset(i * cellW, ptH.coerceIn(4f, size.height - 4f))
                )
            }
        }
    }
}

@Composable
fun PasswordConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val passResult by viewModel.passwordResult.collectAsState()
    val length by viewModel.passLength.collectAsState()
    val context = LocalContext.current
    
    Text("Cryptographically strong keys generator to secure device nodes.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("Crypt Code Width:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("$length bytes", color = themeState.accentColor.color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
    Slider(
        value = length.toFloat(),
        onValueChange = { viewModel.setPassLength(it.toInt()) },
        valueRange = 8f..32f,
        colors = SliderDefaults.colors(thumbColor = themeState.accentColor.color, activeTrackColor = themeState.accentColor.color)
    )
    
    Spacer(modifier = Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(passResult, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { viewModel.generatePassword() },
            modifier = Modifier.weight(1.5f),
            colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color, contentColor = Color.Black)
        ) {
            Text("FORGE KEYS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Button(
            onClick = {
                val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipManager.setPrimaryClip(ClipData.newPlainText("password", passResult))
                Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                viewModel.playTickTone(ToneGenerator.TONE_PROP_BEEP2)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text("COPY", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

@Composable
fun WorldClocksConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val london by viewModel.londonTime.collectAsState()
    val tokyo by viewModel.tokyoTime.collectAsState()
    
    Text("Displays global core execution clock structures in real-time.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp)) {
            Text("LONDON (GMT+1)", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(london, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
        }
        Column(modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp)) {
            Text("TOKYO (JST+9)", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(tokyo, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
        }
    }
}

@Composable
fun QuickNotesConfigurator(viewModel: DashboardViewModel, themeState: ThemeState) {
    val notes by viewModel.notesSandbox.collectAsState()
    var textInput by remember { mutableStateOf(notes) }
    
    Text("Temporary device memory scratchpad. Preserved during session loops.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))
    
    OutlinedTextField(
        value = textInput,
        onValueChange = { 
            textInput = it
            viewModel.updateQuickNotes(it)
        },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = themeState.accentColor.color,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
        )
    )
}

// ==========================================
// BOOT INTRO SEQUENCE RENDERER
// ==========================================

@Composable
fun BootScreen(themeState: ThemeState, onBootFinished: () -> Unit) {
    val logs = remember { mutableStateListOf<String>() }
    var bootProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val bootLogs = listOf(
            "GLYPH_PROTOCORE_REVISION V3.0",
            "CONNECTING PHYSICAL BACKPLANE REAR DRIVERS...",
            "SUCCESS • 4 CORE MODULES SEEDED",
            "MAPPING MONOCHROME PIXEL CHUNKS...",
            "INITIALIZING AUDIO HARMONICS PIPELINE...",
            "GLYPH_OS INITIATED SUCCESSFULLY"
        )
        for (log in bootLogs) {
            logs.add(log)
            delay(500)
            bootProgress += 0.16f
        }
        bootProgress = 1f
        delay(600)
        onBootFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Retro Led pulse blinking
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            // Dynamic Dot Matrix Logo drawn on Canvas!
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rows = 12
                    val cols = 12
                    val cellW = size.width / cols
                    val cellH = size.height / rows
                    val accent = themeState.accentColor.color
                    
                    // G visual representation drawn using individual circular dot matrix segments
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val insideG = (r in 3..9 && c == 3) || // Left stem
                                          (r == 3 && c in 3..9) ||  // Top stem
                                          (r == 9 && c in 3..9) ||  // Bottom stem
                                          (r in 6..9 && c == 9) || // Right bottom
                                          (r == 6 && c in 6..9)    // horizontal bar
                            
                            val isPulseDot = (r == 1 && c == 9) // Simulated red blinking Glyph LED
                            
                            if (isPulseDot) {
                                drawCircle(
                                    color = Color.Red.copy(alpha = pulseAlpha),
                                    radius = cellW / 2.6f,
                                    center = Offset(c * cellW + cellW/2, r * cellH + cellH/2)
                                )
                            } else if (insideG) {
                                drawCircle(
                                    color = accent,
                                    radius = cellW / 3.4f,
                                    center = Offset(c * cellW + cellW/2, r * cellH + cellH/2)
                                )
                            } else {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.04f),
                                    radius = cellW / 5f,
                                    center = Offset(c * cellW + cellW/2, r * cellH + cellH/2)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GLYPH OPERATING SYSTEM",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Text(
                text = "STABLE MONO RUNTIME • DEPLOYED OK",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Diagnostic log feeds
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF0C0C0C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                logs.forEach { log ->
                    Text(
                        text = "• $log",
                        color = themeState.accentColor.color,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = bootProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = themeState.accentColor.color,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onBootFinished) {
                Text("SKIP MATRIX BOOT", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun MacroEditorConfigurator(viewModel: DashboardViewModel, themeState: com.example.models.ThemeState) {
    var newMacroName by remember { mutableStateOf("") }
    val selectedTargets = remember { mutableStateListOf<TileType>() }
    var selectedHaptic by remember { mutableStateOf("TICK") }
    val scenarios by viewModel.scenarios.collectAsState()

    val chainableTargets = listOf(
        TileType.THEATER,
        TileType.CAFFEINE,
        TileType.WIFI,
        TileType.BLUETOOTH,
        TileType.GLYPH,
        TileType.RAM_BOOSTER,
        TileType.SPEED_TEST,
        TileType.FOCUS_TIMER,
        TileType.SCREEN_TIMEOUT
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Subsection 1: Create Custom Scenario
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    "FORGE NEW WORKFLOW SCENARIO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = themeState.accentColor.color,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Name Input
                OutlinedTextField(
                    value = newMacroName,
                    onValueChange = { newMacroName = it },
                    label = { Text("Scenario Name (e.g. Cinema Focus)") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeState.accentColor.color,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = themeState.accentColor.color,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selectable chain targets list
                Text(
                    "CONCURRENT ACTIONS SEQUENCE:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chainableTargets.forEach { type ->
                        val isSelected = selectedTargets.contains(type)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isSelected) selectedTargets.remove(type)
                                    else selectedTargets.add(type)
                                    viewModel.playHapticVibration("TICK")
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked == true) selectedTargets.add(type)
                                    else selectedTargets.remove(type)
                                    viewModel.playHapticVibration("TICK")
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = themeState.accentColor.color,
                                    uncheckedColor = Color.White.copy(alpha = 0.3f),
                                    checkmarkColor = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(type.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type.displayName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Vibration Haptic selector
                Text(
                    "VIBE ACCENT PATTERN:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("TICK", "RUMBLE", "DOUBLE_TAP", "SWEEP").forEach { pat ->
                        val sel = selectedHaptic == pat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) themeState.accentColor.color else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    selectedHaptic = pat
                                    viewModel.playHapticVibration(pat)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                pat.replace("_", " "),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (sel) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Forge Button
                Button(
                    onClick = {
                        if (newMacroName.isBlank()) {
                            viewModel.playHapticVibration("TICK")
                            return@Button
                        }
                        viewModel.addScenario(newMacroName, selectedTargets.toList(), selectedHaptic)
                        newMacroName = ""
                        selectedTargets.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FORGE CHASSIS SCENARIO", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Subsection 2: Deployed Scenarios list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "ACTIVE SCENARIOS DEPLOYMENT GATEWAY",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.6f)
            )

            if (scenarios.isEmpty()) {
                Text(
                    "No custom scenario saved.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f)
                )
            } else {
                scenarios.forEach { sc ->
                    Surface(
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = sc.name.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Vibe Code: ${sc.hapticPattern}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Run Launcher
                                    Button(
                                        onClick = { viewModel.runScenario(sc) },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("DEPLOY", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { viewModel.deleteScenario(sc.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "delete", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Draw targets flow visual string
                            val flowStr = sc.targetTypes.joinToString(" ➔ ") { it.displayName }
                            Text(
                                text = "Flow: $flowStr",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = themeState.accentColor.color.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocalLockoutBodyguardOverlay(
    viewModel: DashboardViewModel,
    themeState: ThemeState,
    timeString: String
) {
    val context = LocalContext.current
    val whitelist by viewModel.whitelistedApps.collectAsState()
    
    // Hold count to override (5 seconds bypass helper)
    var holdTicks by remember { mutableStateOf(0f) }
    val maxTicks = 100f
    var holding by remember { mutableStateOf(false) }

    LaunchedEffect(holding) {
        if (holding) {
            while (holdTicks < maxTicks) {
                delay(30)
                holdTicks += 2f
                if (holdTicks.toInt() % 15 == 0) {
                    try {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(22, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(22)
                        }
                    } catch (e: Exception) {}
                }
            }
            // Toggle focus off!
            val tiles = viewModel.tiles.value
            val ft = tiles.find { it.type == TileType.FOCUS_TIMER }
            if (ft != null) {
                viewModel.triggerTileAction(ft.id)
            }
            holdTicks = 0f
            holding = false
        } else {
            while (holdTicks > 0) {
                delay(15)
                holdTicks = (holdTicks - 4f).coerceAtLeast(0f)
            }
        }
    }

    // Interactive whitelisted app simulations selection
    var simulatedAppSelected by remember { mutableStateOf<String?>(null) }

    val overlayBg = if (themeState.isDarkMode) Color.Black else Color.White
    val contentTint = if (themeState.isDarkMode) Color.White else Color.Black
    val mutedTint = if (themeState.isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    val cardBg = if (themeState.isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayBg)
            .clickable(enabled = true, onClick = {}) // swallow background touch events completely
            .padding(24.dp)
    ) {
        if (simulatedAppSelected == null) {
            // Main Bodyguard Lockdown view
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Indicator Header
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "● DEEP FOCUS BODYGUARD ACTIVE",
                        color = themeState.accentColor.color,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unapproved phone resources blocked to safeguard study flow.",
                        color = mutedTint,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Centered Massive Digital Timer Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .then(Modifier.graphicsLayer(scaleX = 1.1f, scaleY = 1.1f))
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Drawing minimalist bounding dots outer ring
                        Canvas(modifier = Modifier.size(150.dp)) {
                            val r = size.width / 2
                            val cnt = 36
                            for (i in 0 until cnt) {
                                val deg = (i * 360 / cnt).toFloat()
                                val rad = Math.toRadians(deg.toDouble())
                                val startX = center.x + (r - 8.dp.toPx()) * Math.cos(rad).toFloat()
                                val startY = center.y + (r - 8.dp.toPx()) * Math.sin(rad).toFloat()
                                drawCircle(
                                    color = if (i % 6 == 0) themeState.accentColor.color else contentTint.copy(alpha = 0.2f),
                                    radius = if (i % 6 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                                    center = Offset(startX, startY)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeString,
                                color = contentTint,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "REMAINING",
                                color = mutedTint,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "[ MATRIX PROTOCOLS ENGAGED ]",
                        color = themeState.accentColor.color,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Permitted Whitelisted apps deck
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PERMITTED ECOSYSTEM CHANNELS",
                        color = contentTint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        whitelist.forEach { appName ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(1.dp, contentTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { simulatedAppSelected = appName }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val sysIcon = when (appName) {
                                        "Phone" -> Icons.Default.Call
                                        "Messages" -> Icons.Default.Email
                                        "Settings" -> Icons.Default.Settings
                                        "Maps" -> Icons.Default.LocationOn
                                        "Clock" -> Icons.Default.DateRange
                                        "Spotify" -> Icons.Default.PlayArrow
                                        "Calculator" -> Icons.Default.Check
                                        "WhatsApp" -> Icons.Default.Send
                                        "YouTube" -> Icons.Default.PlayArrow
                                        "Chrome" -> Icons.Default.Search
                                        else -> Icons.Default.Info
                                    }
                                    Icon(
                                        imageVector = sysIcon,
                                        contentDescription = appName,
                                        tint = themeState.accentColor.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = appName.uppercase(),
                                        color = contentTint,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tactile press-to-hold emergency override segment
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Red.copy(alpha = 0.08f))
                            .border(1.dp, Color.Red.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .pointerInput(holding) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        holding = event.changes.any { it.pressed }
                                    }
                                }
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = holdTicks / maxTicks,
                                    color = Color.Red,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.fillMaxSize(),
                                    trackColor = Color.Red.copy(alpha = 0.15f)
                                )
                                Text(
                                    text = "${(5 - (holdTicks / maxTicks * 5).toInt()).coerceAtLeast(1)}s",
                                    fontSize = 8.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "PRESS & HOLD TO OVERRIDE LOCKDOWN",
                                color = Color.Red,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        } else {
            // Display Whitelisted Permitted App Simulated UI
            val appToShow = simulatedAppSelected ?: "Phone"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBg)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with back trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { simulatedAppSelected = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = contentTint)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$appToShow Workspace",
                                color = contentTint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeState.accentColor.color.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "BODYGUARD ON",
                                color = themeState.accentColor.color,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Box rendering App's simulator interface
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .padding(16.dp)
                    ) {
                        when (appToShow) {
                            "Phone" -> SimulatedPhoneWorkspace(themeState, contentTint)
                            "Messages" -> SimulatedMessagesWorkspace(themeState, contentTint)
                            "Settings" -> SimulatedSettingsWorkspace(themeState, contentTint)
                            "Maps" -> SimulatedMapsWorkspace(themeState, contentTint)
                            "Clock" -> SimulatedClockWorkspace(themeState, contentTint)
                            else -> SimulatedSoundscapeWorkspace(themeState, contentTint, appToShow)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = { simulatedAppSelected = null },
                        colors = ButtonDefaults.buttonColors(containerColor = contentTint),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SUSPEND $appToShow & RETURN TO BODYGUARD",
                            color = overlayBg,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedPhoneWorkspace(themeState: ThemeState, contentTint: Color) {
    var dialedNumber by remember { mutableStateOf("") }
    var callingState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (callingState) "DIALING VIA BENTO LINK..." else "ZEN SAFE DIALER",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = themeState.accentColor.color
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = dialedNumber.ifEmpty { "Enter Number" },
                fontSize = 26.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                color = if (dialedNumber.isEmpty()) contentTint.copy(alpha = 0.3f) else contentTint
            )
            if (callingState) {
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressIndicator(color = themeState.accentColor.color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Calling connected. Touch Disconnect to abort.", fontSize = 9.sp, color = contentTint.copy(alpha = 0.6f))
            }
        }

        if (!callingState) {
            // Keypad
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val buttons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )
                buttons.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { d ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(contentTint.copy(alpha = 0.08f))
                                    .clickable { dialedNumber += d }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = d, color = contentTint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { dialedNumber = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("CLEAR", color = Color.White, fontSize = 11.sp)
                }
                Button(
                    onClick = { if (dialedNumber.isNotEmpty()) callingState = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color)
                ) {
                    Text("IN-APP CALL", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = { callingState = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("DISCONNECT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimulatedMessagesWorkspace(themeState: ThemeState, contentTint: Color) {
    val messages = remember {
        mutableStateListOf(
            Pair("Carl", "Keep up the studying! Nothing can stop you."),
            Pair("System", "Zen Bodyguard has safely intercepted 14 potential notifications."),
            Pair("Alex", "Let me know when you're done with the deep study session.")
        )
    }
    var currentInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.forEach { pair ->
                val isMe = pair.first == "Me"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMe) themeState.accentColor.color.copy(alpha = 0.25f) else contentTint.copy(alpha = 0.08f))
                            .border(1.dp, if (isMe) themeState.accentColor.color else Color.Transparent, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(text = pair.first, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = themeState.accentColor.color)
                            Text(text = pair.second, fontSize = 12.sp, color = contentTint)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                label = { Text("Tape message...", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeState.accentColor.color,
                    unfocusedBorderColor = contentTint.copy(alpha = 0.3f)
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (currentInput.isNotEmpty()) {
                        messages.add(Pair("Me", currentInput))
                        currentInput = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(themeState.accentColor.color)
            ) {
                Icon(Icons.Default.Send, contentDescription = "send", tint = Color.Black)
            }
        }
    }
}

@Composable
fun SimulatedSettingsWorkspace(themeState: ThemeState, contentTint: Color) {
    var wifiOn by remember { mutableStateOf(true) }
    var bluetoothOn by remember { mutableStateOf(false) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var glyphBrightness by remember { mutableStateOf(0.7f) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PERMITTED ZEN SYSTEM CONTROLS", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Study WiFi Link", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentTint)
                Text("Nothing Net 5G status", fontSize = 10.sp, color = contentTint.copy(alpha = 0.5f))
            }
            Switch(
                checked = wifiOn,
                onCheckedChange = { wifiOn = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeState.accentColor.color,
                    checkedTrackColor = themeState.accentColor.color.copy(alpha = 0.3f)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bluetooth Beacon", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentTint)
                Text("External Audio Receivers", fontSize = 10.sp, color = contentTint.copy(alpha = 0.5f))
            }
            Switch(
                checked = bluetoothOn,
                onCheckedChange = { bluetoothOn = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeState.accentColor.color,
                    checkedTrackColor = themeState.accentColor.color.copy(alpha = 0.3f)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bento Tactile Ticking", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentTint)
                Text("Haptic helper feedback vibration pulse", fontSize = 10.sp, color = contentTint.copy(alpha = 0.5f))
            }
            Switch(
                checked = hapticFeedbackEnabled,
                onCheckedChange = { hapticFeedbackEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeState.accentColor.color,
                    checkedTrackColor = themeState.accentColor.color.copy(alpha = 0.3f)
                )
            )
        }

        Column {
            Text("Glyph Ring Power: ${(glyphBrightness * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentTint)
            Slider(
                value = glyphBrightness,
                onValueChange = { glyphBrightness = it },
                colors = SliderDefaults.colors(
                    thumbColor = themeState.accentColor.color,
                    activeTrackColor = themeState.accentColor.color
                )
            )
        }
    }
}

@Composable
fun SimulatedMapsWorkspace(themeState: ThemeState, contentTint: Color) {
    var compassAngle by remember { mutableStateOf(0f) }
    var locationName by remember { mutableStateOf("Zen Central College") }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            compassAngle = (compassAngle + 1.2f) % 360f
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ZEN VECTOR NAVIGATION SYSTEM", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Destination: $locationName", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = contentTint)
        }

        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(110.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(color = contentTint.copy(alpha = 0.1f))
                
                // Draw coordinate grids
                drawLine(contentTint.copy(alpha = 0.15f), Offset(0f, center.y), Offset(size.width, center.y))
                drawLine(contentTint.copy(alpha = 0.15f), Offset(center.x, 0f), Offset(center.x, size.height))
                
                // Draw compass needle
                rotate(compassAngle, center) {
                    drawTriangleNeedle(this, center, themeState.accentColor.color)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Navigation Guideline: Proceed Straight 400m", fontSize = 11.sp, color = contentTint)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    locationName = if (locationName.contains("Central")) "Nothing Lab Headquarters" else "Zen Central College"
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeState.accentColor.color)
            ) {
                Text("RE-ROUTE DESTINATION", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun drawTriangleNeedle(drawScope: androidx.compose.ui.graphics.drawscope.DrawScope, center: Offset, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - 45.dp.value)
        lineTo(center.x - 8.dp.value, center.y)
        lineTo(center.x + 8.dp.value, center.y)
        close()
    }
    drawScope.drawPath(path, color = color)
}

@Composable
fun SimulatedClockWorkspace(themeState: ThemeState, contentTint: Color) {
    var alarmHour by remember { mutableStateOf(7) }
    var alarmMinute by remember { mutableStateOf(30) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ZEN SAFE TIMING / ALARMS", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%02d:%02d AM", alarmHour, alarmMinute),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = contentTint
            )
            Text("CURRENT ACTIVE ZEN ALARM", fontSize = 10.sp, color = contentTint.copy(alpha = 0.5f))
        }

        Column {
            Text("Set Alarm Hours", fontSize = 11.sp, color = contentTint)
            Slider(
                value = alarmHour.toFloat(),
                onValueChange = { alarmHour = it.toInt() },
                valueRange = 1f..12f,
                colors = SliderDefaults.colors(thumbColor = themeState.accentColor.color)
            )

            Text("Set Alarm Minutes", fontSize = 11.sp, color = contentTint)
            Slider(
                value = alarmMinute.toFloat(),
                onValueChange = { alarmMinute = it.toInt() },
                valueRange = 0f..59f,
                colors = SliderDefaults.colors(thumbColor = themeState.accentColor.color)
            )
        }

        Text("Focus Mode locks system sound: Alarm will pulse haptically.", fontSize = 9.sp, color = contentTint.copy(alpha = 0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun SimulatedSoundscapeWorkspace(themeState: ThemeState, contentTint: Color, appName: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var soundProgress by remember { mutableStateOf(0.4f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(800)
                soundProgress = (soundProgress + 0.02f)
                if (soundProgress >= 1f) soundProgress = 0f
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PERMITTED ZEN ECOSYSTEM AUDIO", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = themeState.accentColor.color)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Channel: $appName Link", fontSize = 12.sp, color = contentTint)
        }

        // Beautiful visualizer bar columns
        Row(
            modifier = Modifier.height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = 8
            for (i in 0 until count) {
                val waveHeight = if (isPlaying) {
                    remember { (10..50).random() }.dp
                } else {
                    12.dp
                }
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(waveHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i % 2 == 0) themeState.accentColor.color else contentTint.copy(alpha = 0.4f))
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Track: Ambient Slate Resonance", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = contentTint)
            Text("Bento Audio Lab - Studio Deep", fontSize = 10.sp, color = contentTint.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = soundProgress,
                color = themeState.accentColor.color,
                trackColor = contentTint.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }
    }
}

