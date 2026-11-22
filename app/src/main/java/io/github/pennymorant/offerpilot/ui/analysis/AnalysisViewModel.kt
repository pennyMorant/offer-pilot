package io.github.pennymorant.offerpilot.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pennymorant.offerpilot.ui.components.AnalysisSelection
import io.github.pennymorant.offerpilot.ui.components.AnalysisUiState
import io.github.pennymorant.offerpilot.ui.components.analysisState
import io.github.pennymorant.offerpilot.work.WorkSessionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class UnifiedAnalysisUiState(
    val regions: AnalysisUiState = AnalysisUiState(),
    val times: AnalysisUiState = AnalysisUiState(),
)

class AnalysisViewModel(session: WorkSessionController) : ViewModel() {
    private val selection = MutableStateFlow(AnalysisSelection())
    private data class Source(
        val statistics: io.github.pennymorant.offerpilot.data.repository.StatisticsSnapshot,
        val deliveries: List<io.github.pennymorant.offerpilot.data.database.projection.DeliverySummary>,
        val currentRegion: String?,
    )
    private val source = session.state.map { Source(it.statistics, it.deliveryHistory, it.currentRegion) }
        .distinctUntilChanged()

    val state = combine(source, selection) { snapshot, selected ->
        buildState(snapshot.statistics, snapshot.deliveries, snapshot.currentRegion, selected)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        buildState(session.state.value.statistics, session.state.value.deliveryHistory,
            session.state.value.currentRegion, selection.value),
    )

    fun select(value: AnalysisSelection) {
        selection.value = value
    }

    private fun buildState(
        snapshot: io.github.pennymorant.offerpilot.data.repository.StatisticsSnapshot,
        deliveries: List<io.github.pennymorant.offerpilot.data.database.projection.DeliverySummary>,
        currentRegion: String?,
        selected: AnalysisSelection,
    ) = UnifiedAnalysisUiState(
        regions = analysisState(snapshot, selected.copy(region = null), byRegion = true,
            deliveryHistory = deliveries, currentRegion = currentRegion),
        times = analysisState(snapshot, selected, byRegion = false, deliveryHistory = deliveries),
    )
}
