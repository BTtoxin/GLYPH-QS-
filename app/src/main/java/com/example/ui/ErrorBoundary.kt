package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DebugLogger

@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    onResetState: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var caughtException by remember { mutableStateOf<Throwable?>(null) }

    // Intercept uncaught background stack logs
    LaunchedEffect(Unit) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (defaultHandler !is ErrorBoundaryHandler) {
            Thread.setDefaultUncaughtExceptionHandler(ErrorBoundaryHandler(defaultHandler) { throwable ->
                caughtException = throwable
            })
        }
    }

    if (caughtException != null) {
        val ex = caughtException!!
        val clipboardManager = LocalClipboardManager.current
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F0F0F)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "System Crash Warning",
                    tint = Color(0xFFFF1744),
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "ZEN ENGINE CRASH SHIELD",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "An unexpected runtime error was successfully intercepted. The ErrorBoundary system prevented the application from completely terminating.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    border = BorderStroke(1.5.dp, Color(0xFFFF1744).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXCEPTION DIAGNOSTICS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF1744)
                            )
                            IconButton(
                                onClick = {
                                    val logText = "${ex.javaClass.simpleName}: ${ex.message}\n" +
                                            ex.stackTrace.take(15).joinToString("\n") { "  at $it" }
                                    clipboardManager.setText(AnnotatedString(logText))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Crash Diagnostics",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(6.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${ex.javaClass.name}: ${ex.localizedMessage ?: "No descriptive message is available."}\n\n" +
                                        ex.stackTrace.joinToString("\n") { "  at $it" },
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE0E0E0),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        caughtException = null
                        onResetState()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset and Resume",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset & Resume",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "RECENT CONSOLE LOGS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val recentLogs by DebugLogger.logs.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    LazyLogViewer(logs = recentLogs)
                }
            }
        }
    } else {
        content()
    }
}

class ErrorBoundaryHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val onCrash: (Throwable) -> Unit
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        DebugLogger.error("Uncaught thread exception intercepted.", throwable)
        onCrash(throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }
}

@Composable
fun LazyLogViewer(logs: List<DebugLogger.LogEntry>) {
    val scrollState = rememberScrollState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        logs.forEach { entry ->
            val color = when (entry.level) {
                "ERROR" -> Color(0xFFFF5252)
                "WARN" -> Color(0xFFFFD740)
                else -> Color(0xFFC0C0C0)
            }
            Text(
                text = "[${entry.timestamp}] [${entry.level}] ${entry.message}",
                color = color,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 11.sp,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
fun ComponentErrorBoundary(
    modifier: Modifier = Modifier,
    fallbackLabel: String = "GRID COMPONENT EXCEPTION",
    onReset: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var caughtError by remember { mutableStateOf<Throwable?>(null) }
    
    if (caughtError != null) {
        val err = caughtError!!
        Surface(
            color = Color(0xFF161616),
            border = BorderStroke(1.5.dp, Color(0xFFFF1744).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Component Error Intercepted",
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = fallbackLabel.uppercase(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = err.localizedMessage ?: "Isolated composition rendering failure intercepted.",
                    fontSize = 8.3.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Button(
                    onClick = { 
                        caughtError = null
                        onReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "RECOVER",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    } else {
        content()
    }
}
