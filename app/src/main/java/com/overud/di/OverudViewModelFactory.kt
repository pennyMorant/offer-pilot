package com.overud.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.overud.ui.dashboard.DashboardViewModel
import com.overud.ui.rules.RulesViewModel

class OverudViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instance = when (modelClass) {
            DashboardViewModel::class.java -> DashboardViewModel(container.session, container.bridge, container.acceptance)
            RulesViewModel::class.java -> RulesViewModel(container.session)
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
        return modelClass.cast(instance) ?: error("ViewModel type mismatch")
    }
}
