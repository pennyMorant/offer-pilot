package com.overud.analytics

import com.overud.model.Offer
import com.overud.model.OfferSource
import com.overud.model.hasVerifiedDistance
import com.overud.model.hasVerifiedDuration
import com.overud.valuation.ValueEngine
import kotlin.math.roundToLong

/** Fixed analysis standard, independent of auto-accept switches and user policy revisions. */
object QualityEvaluator {
    const val VERSION = 1

    fun extract(offer: Offer, previous: QualitySample? = null): QualitySample {
        val amount = offer.amountCents?.takeIf { it > 0 }
        val observedDistance = offer.distanceMeters?.takeIf { it > 0 && offer.hasVerifiedDistance() }
        val observedDuration = offer.durationSeconds?.takeIf { it > 0 && offer.hasVerifiedDuration() }
        // Preserve confirmed features only for aggregation of the same active quote.
        // They are never copied back into the offer used by the auto-accept evaluator.
        val distance = observedDistance ?: previous?.distanceMeters
        val duration = observedDuration ?: previous?.durationSeconds
        val perMile = if (amount != null && distance != null) {
            (amount * ValueEngine.METERS_PER_MILE * 1000 / distance).roundToLong()
        } else null
        val perHour = if (amount != null && duration != null) {
            (amount * 3_600_000.0 / duration).roundToLong()
        } else null
        val quality = when {
            amount == null || perMile == null || perHour == null -> OrderQuality.UNKNOWN
            amount >= 800 && perMile >= 200_000 && perHour >= 2_500_000 -> OrderQuality.GOOD
            amount < 500 || perMile < 100_000 || perHour < 1_500_000 -> OrderQuality.LOW
            else -> OrderQuality.NORMAL
        }
        val inherited = (observedDistance == null && previous?.distanceMeters != null) ||
            (observedDuration == null && previous?.durationSeconds != null)
        val fromOcr = offer.evidence?.source == OfferSource.OCR || (inherited && previous?.fromOcr == true)
        return QualitySample(amount, distance, duration, perMile, perHour, quality, fromOcr)
    }
}
