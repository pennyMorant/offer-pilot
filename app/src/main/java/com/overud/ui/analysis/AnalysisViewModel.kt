package com.overud.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.ui.components.AnalysisSelection
import com.overud.ui.components.AnalysisUiState
import com.overud.ui.components.analysisState
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class UnifiedAnalysisUiState(
    val selection: AnalysisSelection = AnalysisSelection(),
    val regions: AnalysisUiState = AnalysisUiState(),
    val times: AnalysisUiState = AnalysisUiState(),
)

class AnalysisViewModel(session: WorkSessionController) : ViewModel() {
    private val selection = MutableStateFlow(AnalysisSelection())
    private val source = session.state.map { it.statistics to it.deliveryHistory }.distinctUntilChanged()

    val state = combine(source, selection) { snapshot, selected ->
        buildState(snapshot.first, snapshot.second, selected)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        buildState(session.state.value.statistics, session.state.value.deliveryHistory, selection.value),
    )

    fun select(value: AnalysisSelection) {
        selection.value = value
    }

    private fun buildState(
        snapshot: com.overud.data.repository.StatisticsSnapshot,
        deliveries: List<com.overud.data.database.projection.DeliverySummary>,
        selected: AnalysisSelection,
    ) = UnifiedAnalysisUiState(
        selection = selected,
        regions = analysisState(snapshot, selected.copy(region = null), byRegion = true,
            deliveryHistory = deliveries),
        times = analysisState(snapshot, selected, byRegion = false, deliveryHistory = deliveries),
    )
}
