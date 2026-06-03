package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.models.TileType
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodels.DashboardViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: DashboardViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIncomingIntent(intent)
    setContent {
      val tiles by viewModel.tiles.collectAsState()
      val themeState by viewModel.themeState.collectAsState()
      val caffeineEnabled = tiles.find { it.type == TileType.CAFFEINE }?.isActive == true
      val sandboxActive by viewModel.sandboxActive.collectAsState()
      
      if (caffeineEnabled) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }

      androidx.compose.runtime.LaunchedEffect(sandboxActive) {
          try {
              if (sandboxActive) {
                  startLockTask()
              } else {
                  stopLockTask()
              }
          } catch (e: Exception) {
              e.printStackTrace()
          }
      }

      MyApplicationTheme(themeState = themeState) {
        Surface(modifier = Modifier.fillMaxSize()) {
          DashboardScreen(viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: android.content.Intent?) {
    if (intent == null) return
    val blockTriggered = intent.getBooleanExtra("SYSTEM_BLOCK_TRIGGERED", false)
    if (blockTriggered) {
      val blockedPkg = intent.getStringExtra("BLOCKED_PACKAGE_NAME") ?: "App"
      val friendlyName = viewModel.getFriendlyAppName(blockedPkg)
      
      // Vibrate to signal restriction violation
      try {
        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator != null) {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 150), -1))
          } else {
            vibrator.vibrate(200)
          }
        }
      } catch (e: Exception) {}
      
      android.widget.Toast.makeText(
        this, 
        "🚫 Deep Focus Shield: $friendlyName is LOCKED for Zen productivity!", 
        android.widget.Toast.LENGTH_LONG
      ).show()
    }
  }
}
