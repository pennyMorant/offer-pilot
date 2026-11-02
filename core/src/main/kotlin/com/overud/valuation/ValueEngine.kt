package com.overud.valuation

import com.overud.model.*

object ValueEngine {
    const val METERS_PER_MILE = 1609.344

    fun calculate(offer: Offer, policy: Policy): Valuation {
        val effective = policy.effective(offer.platform)
        val prop22 = Prop22Estimator.calculate(offer, policy.prop22)
        if (offer.currency != "USD") return Valuation(null, null, null, prop22 = prop22)
        val amountKnown = offer.reliable || offer.evidence?.amountVerified == true
        val amount = offer.amountCents?.takeIf { it > 0 && amountKnown }
        val miles = offer.distanceMeters?.takeIf { it > 0 && offer.hasVerifiedDistance() }?.div(METERS_PER_MILE)
        val seconds = offer.durationSeconds?.takeIf { it > 0 && offer.hasVerifiedDuration() }
        val cost = if (miles != null && effective.costCentsPerMile >= 0) miles * effective.costCentsPerMile else null
        val contribution = if (amount != null && cost != null) amount - cost else null
        return Valuation(
            centsPerMile = if (miles != null && amount != null) amount / miles else null,
            grossCentsPerHour = if (seconds != null && amount != null) amount * 3600.0 / seconds else null,
            contributionCents = contribution,
            vehicleCostCents = cost,
            contributionCentsPerHour = if (seconds != null && contribution != null) contribution * 3600.0 / seconds else null,
            prop22 = prop22,
        )
    }
}
