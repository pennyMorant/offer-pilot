package com.overud.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.overud.ui.dashboard.DashboardViewModel
import com.overud.ui.rules.RulesViewModel
import com.overud.ui.statistics.StatisticsViewModel
import com.overud.ui.regions.RegionsViewModel

class OverudViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instance = when (modelClass) {
            DashboardViewModel::class.java -> DashboardViewModel(container.session, container.bridge, container.acceptance)
            RulesViewModel::class.java -> RulesViewModel(container.session)
            com.overud.ui.rules.VoiceViewModel::class.java -> com.overud.ui.rules.VoiceViewModel(container.voice)
            StatisticsViewModel::class.java -> StatisticsViewModel(container.session)
            RegionsViewModel::class.java -> RegionsViewModel(container.session)
            com.overud.ui.diagnostics.RuntimeDiagnosticsViewModel::class.java ->
                com.overud.ui.diagnostics.RuntimeDiagnosticsViewModel(container.deviceStatus, container.session, container.bridge)
            com.overud.ui.settings.AppearanceViewModel::class.java -> com.overud.ui.settings.AppearanceViewModel(container.appearance)
            else -> developerViewModel(modelClass, container) ?: error("Unknown ViewModel: ${modelClass.name}")
        }
        return modelClass.cast(instance) ?: error("ViewModel type mismatch")
    }
}
