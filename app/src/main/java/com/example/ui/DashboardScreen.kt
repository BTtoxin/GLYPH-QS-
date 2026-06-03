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
    val tiles by viewModel.tiles.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()
    val themeState by viewModel.themeState.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedSwapId by viewModel.selectedTileIdForSwap.collectAsState()
    val customNames by viewModel.customNames.collectAsState()

    var isBooting by remember { mutableStateOf(true) }

    if (isBooting) {
        BootScreen(themeState) {
            isBooting = false
        }
        return
    }

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
                items(
                    items = tiles,
                    key = { it.id },
                    span = { tile ->
                        val boundedSpan = minOf(tile.size.span, gridMode)
                        GridItemSpan(boundedSpan)
                    }
                ) { tile ->
                    val isSelectedInEdit = selectedSwapId == tile.id
                    val customLabel = customNames[tile.id] ?: tile.type.displayName

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
           this == TileType.QUICK_NOTES
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
        animationSpec = spring()
    )
    
    val shape = when (themeState.tileShape) {
        TileShape.ROUNDED -> RoundedCornerShape(16.dp)
        TileShape.SQUIRCLE -> RoundedCornerShape(26.dp)
        TileShape.SQUARE -> RoundedCornerShape(2.dp)
    }

    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else Color.White,
        animationSpec = spring()
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
            Icon(
                imageVector = tile.type.icon,
                contentDescription = label,
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
            Icon(tile.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
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

    Text("Configure time parameters for the Deep Focus Sandbox. Focus limits device activities.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(12.dp))

    // Interactive Timer Progress
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Timer State:", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(if (isActive) "Active Count" else "Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeState.accentColor.color)
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = { viewModel.triggerTileAction(focusTile?.id ?: "") },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color.Red else themeState.accentColor.color,
            contentColor = Color.Black
        )
    ) {
        Text(if (isActive) "TERMINATE DEEP FOCUS" else "ENGAGE DEEP FOCUS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
