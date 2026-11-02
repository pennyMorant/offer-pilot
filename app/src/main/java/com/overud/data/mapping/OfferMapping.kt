package com.overud.data.mapping

import com.overud.model.*
import com.overud.data.database.entity.OfferRecord

fun Offer.toRecord(decision: Decision, previous: OfferRecord?, region: String?) = OfferRecord().apply {
    id = this@toRecord.id
    platform = this@toRecord.platform.name
    observedAt = previous?.observedAt ?: this@toRecord.observedAt
    amountCents = this@toRecord.amountCents
    distanceMeters = this@toRecord.distanceMeters
    durationSeconds = this@toRecord.durationSeconds
    destination = this@toRecord.destination
    originRegion = previous?.originRegion ?: region
    eligible = decision.eligible
    explanation = decision.reasons.joinToString("；").ifEmpty { "满足所有接单要求" }
    policyVersion = decision.policyVersion
}
