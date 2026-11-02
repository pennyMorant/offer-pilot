package com.overud.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.model.Platform
import com.overud.work.WorkSessionController
import com.overud.work.WorkState
import kotlinx.coroutines.flow.*

class OrdersViewModel(session: WorkSessionController) : ViewModel() {
    val records = session.state.map(::summaries)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), summaries(session.state.value))

    private fun summaries(state: WorkState) = state.records.map { record ->
        OrderSummary(record.id, Platform.entries.find { it.name == record.platform }?.label ?: record.platform,
            record.observedAt, record.amountCents, record.destination, record.eligible, record.explanation, record.policyVersion)
    }
}
