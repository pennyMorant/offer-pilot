package io.github.pennymorant.offerpilot.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardViewModel
import io.github.pennymorant.offerpilot.ui.rules.RulesViewModel
import io.github.pennymorant.offerpilot.ui.analysis.AnalysisViewModel

class OverudViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instance = when (modelClass) {
            DashboardViewModel::class.java -> DashboardViewModel(container.session, container.bridge, container.acceptance, container.arGuard, container::launchPlatform)
            RulesViewModel::class.java -> RulesViewModel(container.session)
            io.github.pennymorant.offerpilot.ui.rules.VoiceViewModel::class.java -> io.github.pennymorant.offerpilot.ui.rules.VoiceViewModel(container.voice)
            AnalysisViewModel::class.java -> AnalysisViewModel(container.session)
            io.github.pennymorant.offerpilot.ui.analysis.DailyReviewViewModel::class.java -> io.github.pennymorant.offerpilot.ui.analysis.DailyReviewViewModel(container.offerHistory)
            io.github.pennymorant.offerpilot.ui.diagnostics.RuntimeDiagnosticsViewModel::class.java ->
                io.github.pennymorant.offerpilot.ui.diagnostics.RuntimeDiagnosticsViewModel(container.deviceStatus, container.session, container.bridge)
            io.github.pennymorant.offerpilot.ui.settings.AppearanceViewModel::class.java -> io.github.pennymorant.offerpilot.ui.settings.AppearanceViewModel(container.appearance)
            else -> developerViewModel(modelClass, container) ?: error("Unknown ViewModel: ${modelClass.name}")
        }
        return modelClass.cast(instance) ?: error("ViewModel type mismatch")
    }
}
