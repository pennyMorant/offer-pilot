package com.overud.ui.regions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.model.Platform
import com.overud.work.WorkSessionController
import com.overud.work.WorkState
import kotlinx.coroutines.flow.*

class RegionsViewModel(session: WorkSessionController) : ViewModel() {
    val state = session.state.map(::summarize)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), summarize(session.state.value))

    private fun summarize(state: WorkState) = RegionsUiState(
        regions = state.regions.map { region ->
            RegionCardState(region.regionKey, Platform.entries.find { it.name == region.platform }?.label ?: region.platform,
                region.offerCount, region.eligibleCount, region.exposureMs / 60000,
                if (region.exposureMs >= 3_600_000 && region.offerCount >= 10) region.offerCount * 3_600_000.0 / region.exposureMs else null)
        },
        destinations = state.records.filter { !it.destination.isNullOrBlank() }.groupBy { it.destination!! }
            .map { (name, offers) -> DestinationSummary(name, offers.size, offers.count { it.eligible }) },
    )
}
