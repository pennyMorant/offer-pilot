package com.overud.ui.settings

import androidx.lifecycle.ViewModel
import com.overud.data.preferences.AppearancePreferences
import com.overud.data.preferences.ThemeMode

class AppearanceViewModel(private val preferences: AppearancePreferences) : ViewModel() {
    val state = preferences.state
    fun mode(value: ThemeMode) = preferences.update(state.value.copy(mode = value))
    fun dynamic(value: Boolean) = preferences.update(state.value.copy(dynamic = value))
}
