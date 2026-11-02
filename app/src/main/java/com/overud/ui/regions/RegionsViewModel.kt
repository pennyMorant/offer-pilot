package com.overud.ui.regions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.ui.components.qualityPresentation
import com.overud.work.WorkSessionController
import com.overud.work.WorkState
import kotlinx.coroutines.flow.*

class RegionsViewModel(session: WorkSessionController) : ViewModel() {
    val state = session.state.map(::summarize)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), summarize(session.state.value))

    private fun summarize(state: WorkState) = RegionsUiState(
        state.statistics.regions.map { qualityPresentation(it, byRegion = true) },
    )
}
