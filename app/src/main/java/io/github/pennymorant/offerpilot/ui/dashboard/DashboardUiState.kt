package io.github.pennymorant.offerpilot.ui.dashboard

import io.github.pennymorant.offerpilot.bridge.BridgeStatus
import io.github.pennymorant.offerpilot.data.preferences.ArGuardState
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.work.WorkState

data class DashboardUiState(
    val working: Boolean,
    val automatic: Boolean,
    val policy: Policy,
    val latest: Offer?,
    val decision: Decision?,
    val message: String,
    val pending: Boolean,
    val connection: BridgeStatus,
    val policySaving: Boolean = false,
    val destinationAssessment: DestinationAssessment = DestinationAssessment(),
    val autoAcceptEnabled: Boolean = false,
    val deliveryPhase: DeliveryPhase = DeliveryPhase.AVAILABLE,
    val activeScene: DrivingScene? = null,
    val arGuard: ArGuardState = ArGuardState(),
    val sessionExclusiveSeen: Int = 0,
    val sessionExclusiveAccepted: Int = 0,
    val backgroundAlert: BackgroundOfferAlert? = null,
) {
    companion object {
        fun from(work: WorkState, bridge: BridgeStatus, arGuard: ArGuardState = ArGuardState()) = DashboardUiState(
            working = work.working,
            automatic = work.automatic,
            policy = work.policy,
            latest = work.latest,
            decision = work.decision,
            message = work.message,
            pending = work.pending,
            connection = bridge,
            policySaving = work.policySaving,
            destinationAssessment = work.destinationAssessment,
            autoAcceptEnabled = work.autoAcceptEnabled,
            deliveryPhase = work.deliveryPhase,
            activeScene = DrivingScene.match(work.policy),
            arGuard = arGuard,
            sessionExclusiveSeen = work.sessionExclusiveSeen,
            sessionExclusiveAccepted = work.sessionExclusiveAccepted,
            backgroundAlert = work.backgroundAlert,
        )
    }
}
