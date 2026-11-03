package com.overud.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.bridge.SheveryBridge
import com.overud.device.DeviceStatus
import com.overud.device.DeviceStatusReader
import com.overud.work.WorkSessionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class RuntimeDiagnosticsViewModel(
    private val reader: DeviceStatusReader,
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
) : ViewModel() {
    private val device = MutableStateFlow(DeviceStatus())
    private var refreshJob: Job? = null
    val rows = combine(session.state, bridge.status, device, ::runtimeDiagnosticRows)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            runtimeDiagnosticRows(session.state.value, bridge.status.value, device.value))

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching { bridge.refresh() }
                runCatching { reader.read() }.getOrDefault(DeviceStatus())
            }
            device.value = snapshot
            // Refresh does not start work or grant permissions; bridge loss still pauses acceptance.
        }
    }
    fun authorize() { bridge.authorize(); refresh() }
}
