package com.overud.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }
data class AppearanceSettings(val mode: ThemeMode = ThemeMode.SYSTEM, val dynamic: Boolean = true)

class AppearancePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(AppearanceSettings(
        ThemeMode.entries.find { it.name == prefs.getString("mode", null) } ?: ThemeMode.SYSTEM,
        prefs.getBoolean("dynamic", true),
    ))
    val state = mutable.asStateFlow()
    fun update(value: AppearanceSettings) {
        prefs.edit().putString("mode", value.mode.name).putBoolean("dynamic", value.dynamic).apply()
        mutable.value = value
    }
}
