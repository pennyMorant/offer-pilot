package io.github.pennymorant.offerpilot.ui.settings

import androidx.lifecycle.ViewModel
import io.github.pennymorant.offerpilot.data.preferences.AppearancePreferences
import io.github.pennymorant.offerpilot.data.preferences.ThemeMode

class AppearanceViewModel(private val preferences: AppearancePreferences) : ViewModel() {
    val state = preferences.state
    fun mode(value: ThemeMode) = preferences.update(state.value.copy(mode = value))
    fun dynamic(value: Boolean) = preferences.update(state.value.copy(dynamic = value))
}
