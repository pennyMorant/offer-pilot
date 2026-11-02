package com.overud.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.analytics.QualitySummaryReducer
import com.overud.ui.components.qualityPresentation
import com.overud.work.WorkSessionController
import com.overud.work.WorkState
import kotlinx.coroutines.flow.*

class OrdersViewModel(session: WorkSessionController) : ViewModel() {
    val cards = session.state.map(::summaries)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), summaries(session.state.value))

    private fun summaries(state: WorkState) = QualitySummaryReducer.group(state.statistics.times, byRegion = false)
        .map { qualityPresentation(it, byRegion = false) }
}
