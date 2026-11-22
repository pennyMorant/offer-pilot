package io.github.pennymorant.offerpilot.work

import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.analytics.WaitingObservation
import io.github.pennymorant.offerpilot.data.database.projection.DeliverySummary
import io.github.pennymorant.offerpilot.data.repository.StatisticsSnapshot

data class WorkState(
    val working: Boolean = false,
    val diagnosticActive: Boolean = false,
    val automatic: Boolean = false,
    val autoAcceptEnabled: Boolean = false,
    val policy: Policy = Policy(),
    val latest: Offer? = null,
    val currentPage: PlatformPage? = null,
    val decision: Decision? = null,
    val deliveryHistory: List<DeliverySummary> = emptyList(),
    val waitingHistory: List<WaitingObservation> = emptyList(),
    val statistics: StatisticsSnapshot = StatisticsSnapshot(),
    val message: String = "开始工作后，将自动分析新报价",
    val pending: Boolean = false,
    val lastCaptureAt: Long? = null,
    val currentRegion: String? = null,
    val policySaving: Boolean = false,
    val policySaveError: String? = null,
    val destinationEvidence: DestinationEvidence? = null,
    val destinationAssessment: DestinationAssessment = DestinationAssessment(),
    val deliveryPhase: DeliveryPhase = DeliveryPhase.AVAILABLE,
    val activePlatform: Platform? = null,
    val lastCompletedPlatform: Platform? = null,
    val lastCompletedAt: Long? = null,
    val sessionExclusiveSeen: Int = 0,
    val sessionExclusiveAccepted: Int = 0,
    val backgroundAlert: BackgroundOfferAlert? = null,
)
