package com.relatopro.app.ui.theme

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ThemeManager {
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
            _themeMode.value = prefs.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM"
            isInitialized = true
        }
    }

    fun setThemeMode(context: Context, mode: String) {
        if (_themeMode.value == mode) return
        // Instant synchronous in-memory update for 0ms UI responsiveness
        _themeMode.value = mode
        // Non-blocking asynchronous persistence
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("app_theme_mode", mode).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleTheme(context: Context, isCurrentlyDark: Boolean) {
        setThemeMode(context, if (isCurrentlyDark) "LIGHT" else "DARK")
    }
}
