package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FocusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
            val wasActive = prefs.getBoolean("tile_active_focus_timer", false)
            if (wasActive) {
                val remainingSecs = prefs.getInt("focus_seconds_remaining", 25 * 60)
                val minutes = (remainingSecs / 60).coerceAtLeast(1)
                
                val serviceIntent = Intent(context, StrictFocusService::class.java).apply {
                    action = StrictFocusService.ACTION_START
                    putExtra("focus_minutes", minutes)
                }
                
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
