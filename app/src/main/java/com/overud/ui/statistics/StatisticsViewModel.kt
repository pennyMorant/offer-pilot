package com.overud.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.ui.components.*
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*

class StatisticsViewModel(session: WorkSessionController) : ViewModel() {
    private val selection = MutableStateFlow(AnalysisSelection())
    val state = combine(session.state.map { it.statistics }.distinctUntilChanged(), selection) { snapshot, selected ->
        analysisState(snapshot, selected, byRegion = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        analysisState(session.state.value.statistics, selection.value, byRegion = false))

    fun select(value: AnalysisSelection) { selection.value = value }
}
