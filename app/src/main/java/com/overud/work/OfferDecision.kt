package com.overud.work

import com.overud.model.Decision
import com.overud.model.Offer
import com.overud.valuation.EligibilityEvaluator

/** A changed aggregate snapshot invalidates previously calculated regional evidence. */
fun WorkState.assessOffer(offer: Offer, now: Long): Decision {
    val evidence = destinationEvidence?.takeIf { it.historyRevision == statistics.revision }
    return EligibilityEvaluator.assess(offer, policy, now, evidence)
}
