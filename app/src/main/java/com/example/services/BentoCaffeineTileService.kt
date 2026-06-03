package com.example.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Context
import android.widget.Toast
import android.content.Intent

class BentoCaffeineTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        val isCaffeine = prefs.getBoolean("caffeine_multiplier", false)
        tile.state = if (isCaffeine) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Caffeine Awake"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        val isCaffeine = prefs.getBoolean("caffeine_multiplier", false)
        val newCaffeineState = !isCaffeine
        
        prefs.edit().apply {
            putBoolean("caffeine_multiplier", newCaffeineState)
            putBoolean("tile_active_change_flag", true)
            apply()
        }
        
        tile.state = if (newCaffeineState) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
        
        val msg = if (newCaffeineState) "Caffeine Awake Engaged" else "Caffeine Awake Disengaged"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

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
