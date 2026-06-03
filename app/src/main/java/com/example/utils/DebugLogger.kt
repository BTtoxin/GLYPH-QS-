package com.example.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    data class LogEntry(
        val timestamp: String,
        val level: String, // "INFO", "WARN", "ERROR"
        val message: String,
        val throwable: Throwable? = null
    )

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(
                timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
                level = "INFO",
                message = "Debug Logger Initialized: Listening for console errors & exceptions."
            )
        )
    )
    val logs = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(level: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            message = message,
            throwable = throwable
        )
        _logs.update { (it + entry).takeLast(100) } // Keep last 100 logs
        
        // Also print to Android system logcat for standard debugging
        if (level == "ERROR") {
            android.util.Log.e("BentoZenQS", "$message", throwable)
        } else if (level == "WARN") {
            android.util.Log.w("BentoZenQS", "$message")
        } else {
            android.util.Log.i("BentoZenQS", "$message")
        }
    }

    fun info(message: String) = log("INFO", message)
    fun warn(message: String) = log("WARN", message)
    fun error(message: String, throwable: Throwable? = null) {
        log("ERROR", message, throwable)
    }

    fun clear() {
        _logs.value = listOf(
            LogEntry(
                timestamp = dateFormat.format(Date()),
                level = "INFO",
                message = "Logs cleared by user."
            )
        )
    }
}
