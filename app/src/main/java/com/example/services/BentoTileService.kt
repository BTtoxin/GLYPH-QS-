package com.example.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Context
import android.widget.Toast
import android.content.Intent

class BentoTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        val isDeepFocus = prefs.getBoolean("tile_active_focus_timer", false)
        tile.state = if (isDeepFocus) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Bento Zen Focus"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        val isDeepFocus = prefs.getBoolean("tile_active_focus_timer", false)
        val newFocusState = !isDeepFocus
        
        prefs.edit().apply {
            putBoolean("tile_active_focus_timer", newFocusState)
            // also trigger state active helper
            putBoolean("tile_active_change_flag", true)
            apply()
        }
        
        tile.state = if (newFocusState) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
        
        val mode = if (newFocusState) "Engaged (Zen Mode active)" else "Disengaged"
        Toast.makeText(this, "Bento Zen Focus: $mode", Toast.LENGTH_SHORT).show()
        
        // Launch/resume MainActivity to show bodyguard overlays
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            try {
                startActivityAndCollapse(launchIntent)
            } catch (e: Exception) {
                // Fallback direct launch if activity launch permission requires it
                val intent = Intent(this, Class.forName("com.example.MainActivity"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }
}
