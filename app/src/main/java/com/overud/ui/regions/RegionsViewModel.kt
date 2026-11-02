package com.overud.ui.regions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.ui.components.*
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*

class RegionsViewModel(session: WorkSessionController) : ViewModel() {
    private val selection = MutableStateFlow(AnalysisSelection())
    val state = combine(session.state.map { it.statistics }.distinctUntilChanged(), selection) { snapshot, selected ->
        analysisState(snapshot, selected, byRegion = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        analysisState(session.state.value.statistics, selection.value, byRegion = true))

    fun select(value: AnalysisSelection) { selection.value = value.copy(region = null) }
}
