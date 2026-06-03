package com.example.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Context
import android.widget.Toast
import android.content.Intent

class BentoThemeTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("is_dark_mode", true)
        tile.state = if (isDarkMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Bento Theme Mode"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("is_dark_mode", true)
        val newDarkMode = !isDarkMode
        
        prefs.edit().apply {
            putBoolean("is_dark_mode", newDarkMode)
            putBoolean("tile_active_change_flag", true)
            apply()
        }
        
        tile.state = if (newDarkMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
        
        val mode = if (newDarkMode) "Dark Slate" else "Light Crisp"
        Toast.makeText(this, "Theme toggled via OS: $mode", Toast.LENGTH_SHORT).show()

        // Sync with main app by waking up the workspace
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            try {
                startActivityAndCollapse(launchIntent)
            } catch (e: Exception) {
                // fall back
            }
        }
    }
}
