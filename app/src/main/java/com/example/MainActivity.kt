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
    setContent {
      val tiles by viewModel.tiles.collectAsState()
      val themeState by viewModel.themeState.collectAsState()
      val caffeineEnabled = tiles.find { it.type == TileType.CAFFEINE }?.isActive == true
      
      if (caffeineEnabled) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }

      MyApplicationTheme(darkTheme = themeState.isDarkMode, accentColor = themeState.accentColor.color) {
        Surface(modifier = Modifier.fillMaxSize()) {
          DashboardScreen(viewModel)
        }
      }
    }
  }
}
