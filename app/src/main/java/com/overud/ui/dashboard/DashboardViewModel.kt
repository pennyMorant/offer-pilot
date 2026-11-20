package com.overud.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.data.preferences.ArGuardPreferences
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*

class DashboardViewModel(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
    private val acceptance: AutoAcceptController,
    private val arGuard: ArGuardPreferences,
    private val launchPlatform: (com.overud.model.Platform) -> Unit,
) : ViewModel() {
    val state = combine(session.state, bridge.status, arGuard.state) { work, connection, ar ->
        DashboardUiState.from(work, connection, ar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        DashboardUiState.from(session.state.value, bridge.status.value, arGuard.state.value))
    fun setAutomatic(enabled: Boolean) = session.setAutomatic(enabled)
    fun authorize() = bridge.authorize()
    fun resolvePending() = acceptance.resolvePending()
    fun applyScene(scene: com.overud.model.DrivingScene) =
        session.savePolicy(scene.applyTo(session.state.value.policy))
    fun adjustUberAr(delta: Int) = arGuard.adjustUberAr(delta)
    fun openPlatform(platform: com.overud.model.Platform) = launchPlatform(platform)
}
