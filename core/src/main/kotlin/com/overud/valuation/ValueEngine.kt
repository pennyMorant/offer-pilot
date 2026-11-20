package com.overud.valuation

import com.overud.model.*
import kotlin.math.roundToInt

object ValueEngine {
    const val METERS_PER_MILE = 1609.344

    fun calculate(offer: Offer, policy: Policy, calibration: DurationCalibration? = null): Valuation {
        val effective = policy.effective(offer.platform)
        val prop22 = Prop22Estimator.calculate(offer, policy.prop22)
        val confidence = confidence(offer)
        if (offer.currency != "USD") return Valuation(null, null, null,
            confidence = confidence, prop22 = prop22)
        val amountKnown = offer.reliable || offer.evidence?.amountVerified == true
        val amount = offer.amountCents?.takeIf { it > 0 && amountKnown }
        val miles = offer.distanceMeters?.takeIf { it > 0 && offer.hasVerifiedDistance() }?.div(METERS_PER_MILE)
        val quotedSeconds = offer.durationSeconds?.takeIf { it > 0 && offer.hasVerifiedDuration() }
        val usableCalibration = calibration?.takeIf(DurationCalibration::valid)
        val baseSeconds = quotedSeconds?.let { quoted ->
            usableCalibration?.let { (quoted * it.ratio.coerceAtLeast(1.0)).toLong() } ?: quoted
        }
        val shopFloorSeconds = if (offer.serviceKind == OfferService.SHOP_AND_PAY) {
            offer.itemCount?.takeIf { it > 0 }?.let { it * 45L + 600L }
        } else null
        val seconds = when {
            baseSeconds != null && shopFloorSeconds != null -> maxOf(baseSeconds, shopFloorSeconds)
            else -> baseSeconds
        }
        val cost = if (miles != null && effective.costCentsPerMile >= 0) miles * effective.costCentsPerMile else null
        val contribution = if (amount != null && cost != null) amount - cost else null
        val contributionPerMile = if (miles != null && contribution != null) contribution / miles else null
        val contributionPerHour = if (seconds != null && contribution != null) contribution * 3600.0 / seconds else null
        val scoreComponents = scoreComponents(contribution, contributionPerMile, contributionPerHour, effective)
        val score = scoreComponents?.let {
            (it.contributionHourly * 0.50 + it.contributionPerMile * 0.35 +
                it.contributionAmount * 0.15).roundToInt().coerceIn(0, 100)
        }
        return Valuation(
            centsPerMile = if (miles != null && amount != null) amount / miles else null,
            grossCentsPerHour = if (seconds != null && amount != null) amount * 3600.0 / seconds else null,
            contributionCents = contribution,
            vehicleCostCents = cost,
            contributionCentsPerMile = contributionPerMile,
            contributionCentsPerHour = contributionPerHour,
            effectiveDurationSeconds = seconds,
            timeBasis = if (usableCalibration == null) TimeEstimateBasis.QUOTED else TimeEstimateBasis.CALIBRATED,
            score = score,
            grade = grade(score),
            confidence = confidence,
            scoreComponents = scoreComponents,
            prop22 = prop22,
        )
    }

    private fun scoreComponents(amount: Double?, mile: Double?, hour: Double?, rules: Policy): ScoreComponents? {
        if (amount == null || mile == null || hour == null) return null
        return ScoreComponents(
            ratioScore(hour / rules.targetContributionCentsPerHour),
            ratioScore(mile / rules.targetContributionCentsPerMile),
            ratioScore(amount / rules.targetContributionCents),
        )
    }

    private fun ratioScore(ratio: Double): Double = when {
        !ratio.isFinite() || ratio <= 0 -> 0.0
        ratio <= 1.0 -> ratio * 70.0
        ratio <= 1.5 -> 70.0 + (ratio - 1.0) * 60.0
        else -> 100.0
    }

    private fun grade(score: Int?) = when (score) {
        null -> ValueGrade.UNKNOWN
        in 85..100 -> ValueGrade.EXCELLENT
        in 70..84 -> ValueGrade.GOOD
        in 50..69 -> ValueGrade.BORDERLINE
        else -> ValueGrade.POOR
    }

    private fun confidence(offer: Offer): EvaluationConfidence {
        val evidence = offer.evidence
        val core = offer.reliable && evidence?.source == OfferSource.UI_NODES &&
            evidence.amountVerified && offer.hasVerifiedDistance() && offer.hasVerifiedDuration()
        if (core) return if (offer.destination.isNullOrBlank()) EvaluationConfidence.MEDIUM
            else EvaluationConfidence.HIGH
        if (evidence?.source == OfferSource.OCR || evidence?.amountVerified == true) return EvaluationConfidence.LOW
        return EvaluationConfidence.UNKNOWN
    }
}
