package com.overud.ui.dashboard

import androidx.lifecycle.ViewModel
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.work.WorkSessionController

class DashboardViewModel(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
    private val acceptance: AutoAcceptController,
) : ViewModel() {
    val state = session.state
    val connection = bridge.status
    fun setAutomatic(enabled: Boolean) = session.setAutomatic(enabled)
    fun authorize() = bridge.authorize()
    fun resolvePending() = acceptance.resolvePending()
}
