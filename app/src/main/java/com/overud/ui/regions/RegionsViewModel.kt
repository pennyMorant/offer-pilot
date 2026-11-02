package com.overud.ui.regions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.ui.components.*
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*

class RegionsViewModel(session: WorkSessionController) : ViewModel() {
    private val selection = MutableStateFlow(AnalysisSelection())
    val state = combine(session.state.map { it.statistics to it.deliveryHistory }.distinctUntilChanged(), selection) { snapshot, selected ->
        analysisState(snapshot.first, selected, byRegion = true, deliveryHistory = snapshot.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        analysisState(session.state.value.statistics, selection.value, byRegion = true,
            deliveryHistory = session.state.value.deliveryHistory))

    fun select(value: AnalysisSelection) { selection.value = value.copy(region = null) }
}
