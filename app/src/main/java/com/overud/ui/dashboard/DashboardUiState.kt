package com.overud.ui.dashboard

import com.overud.bridge.BridgeStatus
import com.overud.model.*
import com.overud.work.WorkState

data class DashboardUiState(
    val working: Boolean,
    val automatic: Boolean,
    val policy: Policy,
    val latest: Offer?,
    val decision: Decision?,
    val message: String,
    val pending: Boolean,
    val recordCount: Int,
    val eligibleCount: Int,
    val connection: BridgeStatus,
) {
    companion object {
        fun from(work: WorkState, bridge: BridgeStatus) = DashboardUiState(
            work.working, work.automatic, work.policy, work.latest, work.decision,
            work.message, work.pending, work.records.size, work.records.count { it.eligible }, bridge,
        )
    }
}
