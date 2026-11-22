package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pennymorant.offerpilot.acceptance.AutoAcceptController
import io.github.pennymorant.offerpilot.bridge.SheveryBridge
import io.github.pennymorant.offerpilot.data.preferences.ArGuardPreferences
import io.github.pennymorant.offerpilot.work.WorkSessionController
import kotlinx.coroutines.flow.*

class DashboardViewModel(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
    private val acceptance: AutoAcceptController,
    private val arGuard: ArGuardPreferences,
    private val launchPlatform: (io.github.pennymorant.offerpilot.model.Platform) -> Unit,
) : ViewModel() {
    val state = combine(session.state, bridge.status, arGuard.state) { work, connection, ar ->
        DashboardUiState.from(work, connection, ar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        DashboardUiState.from(session.state.value, bridge.status.value, arGuard.state.value))
    fun setAutomatic(enabled: Boolean) = session.setAutomatic(enabled)
    fun authorize() = bridge.authorize()
    fun resolvePending() = acceptance.resolvePending()
    fun applyScene(scene: io.github.pennymorant.offerpilot.model.DrivingScene) =
        session.savePolicy(scene.applyTo(session.state.value.policy))
    fun adjustUberAr(delta: Int) = arGuard.adjustUberAr(delta)
    fun openPlatform(platform: io.github.pennymorant.offerpilot.model.Platform) = launchPlatform(platform)
}
