package com.overud.valuation

import com.overud.model.*
import kotlin.math.roundToInt

object ValueEngine {
    const val METERS_PER_MILE = 1609.344

    fun calculate(
        offer: Offer,
        policy: Policy,
        calibration: DurationCalibration? = null,
        destinationEvidence: DestinationEvidence? = null,
    ): Valuation {
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
        val seconds = effectiveSeconds(offer, quotedSeconds, usableCalibration)
        val cost = if (miles != null && effective.costCentsPerMile >= 0) miles * effective.costCentsPerMile else null
        val contribution = if (amount != null && cost != null) amount - cost else null
        val contributionPerMile = if (miles != null && contribution != null) contribution / miles else null
        val contributionPerHour = if (seconds != null && contribution != null) contribution * 3600.0 / seconds else null

        val weakDeadhead = destinationEvidence?.outlook == RegionalOutlook.WEAK && miles != null && miles > 6.0
        val evalMiles = if (weakDeadhead) miles!! * 1.35 else miles
        val evalSeconds = if (weakDeadhead && seconds != null) (seconds * 1.30).toLong() else seconds
        val evalCost = if (evalMiles != null && effective.costCentsPerMile >= 0) evalMiles * effective.costCentsPerMile else null
        val evalContribution = if (amount != null && evalCost != null) amount - evalCost else contribution
        val evalPerMile = if (evalMiles != null && evalContribution != null) evalContribution / evalMiles else contributionPerMile
        val evalPerHour = if (evalSeconds != null && evalContribution != null) evalContribution * 3600.0 / evalSeconds else contributionPerHour

        val scoreComponents = scoreComponents(evalContribution, evalPerMile, evalPerHour, effective)
        val notes = mutableListOf<String>()
        val rawScore = scoreComponents?.let { weightedScore(it, DrivingScene.match(policy)) }
        val penalty = rawScore?.let {
            bottleneckPenalty(offer, miles, evalPerMile, evalPerHour, effective, weakDeadhead, destinationEvidence, notes)
        } ?: 0
        val score = rawScore?.let { (it - penalty).coerceIn(0, 100) }
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
            scoreNotes = notes,
            deadheadAdjusted = weakDeadhead,
        )
    }

    private fun effectiveSeconds(offer: Offer, quotedSeconds: Long?, calibration: DurationCalibration?): Long? {
        val base = quotedSeconds?.let { quoted ->
            calibration?.let { (quoted * it.ratio.coerceAtLeast(1.0)).toLong() } ?: quoted
        } ?: return null
        val shopFloor = if (offer.serviceKind == OfferService.SHOP_AND_PAY) {
            val items = offer.itemCount?.takeIf { it > 0 }
            if (items != null) {
                val tier1 = minOf(items, 10) * 35L
                val tier2 = (items - 10).coerceAtLeast(0) * 45L
                300L + tier1 + tier2 + 600L
            } else base + 300L
        } else 0L
        val stackedTax = if (offer.isStacked) 360L else 0L
        return maxOf(base + stackedTax, shopFloor)
    }

    private fun weightedScore(c: ScoreComponents, scene: DrivingScene?): Int = when (scene) {
        DrivingScene.PEAK_CHERRY_PICK -> (c.contributionHourly * 0.45 + c.contributionPerMile * 0.40 + c.contributionAmount * 0.15)
        DrivingScene.OFF_PEAK_GOLD -> (c.contributionHourly * 0.45 + c.contributionPerMile * 0.35 + c.contributionAmount * 0.20)
        else -> (c.contributionHourly * 0.50 + c.contributionPerMile * 0.35 + c.contributionAmount * 0.15)
    }.roundToInt()

    private fun bottleneckPenalty(
        offer: Offer,
        miles: Double?,
        perMile: Double?,
        perHour: Double?,
        rules: Policy,
        weakDeadhead: Boolean,
        destinationEvidence: DestinationEvidence?,
        notes: MutableList<String>,
    ): Int {
        var penalty = 0
        if (weakDeadhead) notes += "弱区长途含回程折算"
        else if (destinationEvidence?.outlook == RegionalOutlook.GOOD) notes += "目的地历史优质区"
        if (perHour != null && perHour < rules.targetContributionCentsPerHour * 0.55) {
            penalty += 12
            notes += "有效时薪偏低(-12)"
        }
        if (miles != null && miles > 10.0 && perMile != null && perMile < 120.0) {
            penalty += 15
            notes += "超长途低单价(-15)"
        }
        if (offer.preferredTag && ((perMile != null && perMile < 150.0) ||
                (perHour != null && perHour < rules.targetContributionCentsPerHour * 0.75))) {
            penalty += 8
            notes += "优选标签低效(-8)"
        }
        return penalty
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
