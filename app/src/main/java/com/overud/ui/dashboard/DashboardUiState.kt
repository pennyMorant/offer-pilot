package com.overud.ui.dashboard

import com.overud.bridge.BridgeStatus
import com.overud.data.preferences.ArGuardState
import com.overud.model.*
import com.overud.work.WorkState
import com.overud.ui.components.platformPagePresentation

data class DashboardUiState(
    val working: Boolean,
    val automatic: Boolean,
    val policy: Policy,
    val latest: Offer?,
    val decision: Decision?,
    val message: String,
    val pending: Boolean,
    val recordCount: Long,
    val goodCount: Long,
    val connection: BridgeStatus,
    val policySaving: Boolean = false,
    val policySummaries: List<PlatformPolicySummary> = emptyList(),
    val destinationAssessment: DestinationAssessment = DestinationAssessment(),
    val autoAcceptEnabled: Boolean = false,
    val deliveryPhase: DeliveryPhase = DeliveryPhase.AVAILABLE,
    val currentPageLabel: String? = null,
    val activeScene: DrivingScene? = null,
    val arGuard: ArGuardState = ArGuardState(),
    val sessionExclusiveSeen: Int = 0,
    val sessionExclusiveAccepted: Int = 0,
) {
    companion object {
        fun from(work: WorkState, bridge: BridgeStatus, arGuard: ArGuardState = ArGuardState()) = DashboardUiState(
            work.working, work.automatic, work.policy, work.latest, work.decision,
            work.message, work.pending, work.statistics.totals.sumOf { it.offerCount }, work.statistics.totals.sumOf { it.goodCount }, bridge,
            policySaving = work.policySaving,
            policySummaries = policySummaries(work.policy),
            destinationAssessment = work.destinationAssessment,
            autoAcceptEnabled = work.autoAcceptEnabled,
            deliveryPhase = work.deliveryPhase,
            currentPageLabel = platformPagePresentation(work.currentPage),
            activeScene = DrivingScene.match(work.policy),
            arGuard = arGuard,
            sessionExclusiveSeen = work.sessionExclusiveSeen,
            sessionExclusiveAccepted = work.sessionExclusiveAccepted,
        )
    }
}
