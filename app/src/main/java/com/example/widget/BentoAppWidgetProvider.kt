package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.RemoteViews
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.R

class BentoAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_FOCUS = "com.example.widget.ACTION_WIDGET_FOCUS"
        const val ACTION_WIDGET_PURGE = "com.example.widget.ACTION_WIDGET_PURGE"
        const val ACTION_WIDGET_THEME = "com.example.widget.ACTION_WIDGET_THEME"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = context.getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        
        when (intent.action) {
            ACTION_WIDGET_FOCUS -> {
                val current = prefs.getBoolean("tile_active_focus_timer", false)
                val newFocus = !current
                prefs.edit().apply {
                    putBoolean("tile_active_focus_timer", newFocus)
                    putBoolean("tile_active_change_flag", true)
                    apply()
                }
                val modeStr = if (newFocus) "Zen Focus Engaged!" else "Zen Focus Disengaged!"
                Toast.makeText(context, modeStr, Toast.LENGTH_SHORT).show()
                triggerLocalWidgetUpdate(context)
            }
            ACTION_WIDGET_PURGE -> {
                try {
                    val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipManager.setPrimaryClip(ClipData.newPlainText("", ""))
                    Toast.makeText(context, "Sensitive Clipboard Purged!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Clipboard Purged", Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_WIDGET_THEME -> {
                val isDark = prefs.getBoolean("is_dark_mode", true)
                prefs.edit().putBoolean("is_dark_mode", !isDark).apply()
                val themeStr = if (isDark) "Light Theme Selected" else "Dark Theme Selected"
                Toast.makeText(context, themeStr, Toast.LENGTH_SHORT).show()
                triggerLocalWidgetUpdate(context)
            }
        }
    }

    private fun triggerLocalWidgetUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, BentoAppWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.bento_widget_layout)
        val prefs = context.getSharedPreferences("glyph_qs_prefs", Context.MODE_PRIVATE)
        
        val isDeepFocus = prefs.getBoolean("tile_active_focus_timer", false)
        val isDarkMode = prefs.getBoolean("is_dark_mode", true)

        // Bind data state to RemoteViews
        views.setTextViewText(R.id.widget_status_value, if (isDeepFocus) "Active Mode" else "Off (Standby)")
        views.setTextViewText(R.id.widget_active_tag, if (isDeepFocus) "LOCKED" else "STANDBY")
        views.setTextViewText(R.id.widget_detail_label, "Theme: " + (if (isDarkMode) "Dark Slate" else "Light Crisp"))
        
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        views.setTextViewText(R.id.widget_updated_time, "Sync: $timeNow")

        // Hook up clicks
        views.setOnClickPendingIntent(R.id.widget_btn_focus, getPendingSelfIntent(context, ACTION_WIDGET_FOCUS))
        views.setOnClickPendingIntent(R.id.widget_btn_purge, getPendingSelfIntent(context, ACTION_WIDGET_PURGE))
        views.setOnClickPendingIntent(R.id.widget_btn_theme, getPendingSelfIntent(context, ACTION_WIDGET_THEME))

        // Instruct widget manager to update
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, BentoAppWidgetProvider::class.java).apply {
            this.action = action
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
