package io.github.pennymorant.offerpilot.work

import io.github.pennymorant.offerpilot.model.Decision
import io.github.pennymorant.offerpilot.model.Offer
import io.github.pennymorant.offerpilot.valuation.EligibilityEvaluator
import io.github.pennymorant.offerpilot.analytics.DeliveryCalibrationSelector

/** A changed aggregate snapshot invalidates previously calculated regional evidence. */
fun WorkState.assessOffer(offer: Offer, now: Long): Decision {
    val evidence = destinationEvidence?.takeIf { it.historyRevision == statistics.revision }
    val calibration = DeliveryCalibrationSelector.select(offer, currentRegion, deliveryHistory)
    return EligibilityEvaluator.assess(offer, policy, now, evidence, calibration)
}
