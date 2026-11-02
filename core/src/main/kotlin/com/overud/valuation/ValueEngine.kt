package com.overud.valuation

import com.overud.model.*

object ValueEngine {
    const val METERS_PER_MILE = 1609.344
    fun calculate(offer: Offer, policy: Policy): Valuation {
        val effective = policy.effective(offer.platform)
        val totalsKnown = offer.reliable || offer.evidence?.totalsVerified == true
        val miles = offer.distanceMeters?.takeIf { it > 0 && totalsKnown }?.div(METERS_PER_MILE)
        val seconds = offer.durationSeconds?.takeIf { it > 0 && totalsKnown }
        return Valuation(
            if (miles != null && offer.amountCents != null) offer.amountCents / miles else null,
            if (seconds != null && offer.amountCents != null) offer.amountCents * 3600.0 / seconds else null,
            if (miles != null && offer.amountCents != null) offer.amountCents - miles * effective.costCentsPerMile else null,
        )
    }
}
