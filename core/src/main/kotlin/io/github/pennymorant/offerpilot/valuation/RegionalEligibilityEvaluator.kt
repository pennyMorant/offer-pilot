package io.github.pennymorant.offerpilot.valuation

import io.github.pennymorant.offerpilot.analytics.QualityEvaluator
import io.github.pennymorant.offerpilot.model.*
import java.time.ZoneId

/** A historical quality requirement; never an arrival probability or waiting prediction. */
object RegionalEligibilityEvaluator {
    fun reasons(offer: Offer, rules: RegionalRequirement, evidence: DestinationEvidence?, now: Long): List<String> {
        if (!rules.enabled) return emptyList()
        if (!rules.valid()) return listOf("区域质量要求无效")
        val duration = offer.durationSeconds
        val totalsKnown = offer.hasVerifiedDuration()
        if (!totalsKnown || duration == null || duration !in 1..86400 || offer.isAddon) {
            return listOf("缺少可信完整送达耗时，无法检查目的地区域要求")
        }
        if (evidence == null || evidence.token != offer.id || evidence.platform != offer.platform ||
            evidence.destination != offer.destination || evidence.durationSeconds != duration ||
            evidence.observedMinute != offer.observedAt / 60000 * 60000 ||
            now - evidence.calculatedAt !in 0..30_000 || evidence.qualityVersion != QualityEvaluator.VERSION) {
            return listOf("等待当前报价的目的地区域证据")
        }
        val assessment = evidence.assessment
        if (assessment.resolution != DestinationResolution.RESOLVED || assessment.regionKey == null ||
            assessment.precision !in setOf(DestinationPrecision.COORDINATE, DestinationPrecision.ADDRESS)) {
            return listOf("目的地未精确解析到地址或坐标，区域要求未通过")
        }
        val timeScopes = setOf(RegionalEvidenceScope.SAME_HOUR, RegionalEvidenceScope.ADJACENT_HOURS,
            RegionalEvidenceScope.SAME_HOUR_OTHER_DAYS)
        if (assessment.evidenceScope !in timeScopes || assessment.analysisZoneId != ZoneId.systemDefault().id ||
            assessment.arrivalAt != evidence.observedMinute + duration * 1000) {
            return listOf("缺少目的地所在网格的送达时段证据")
        }
        val goodRate = assessment.goodRate
        if (assessment.outlook == RegionalOutlook.INSUFFICIENT ||
            assessment.classifiedCount < rules.minimumClassified || assessment.classifiedCount > assessment.observedCount ||
            assessment.classifiedCount < assessment.observedCount / 2.0 ||
            goodRate == null || !goodRate.isFinite() || goodRate !in 0.0..1.0) {
            return listOf("目的地区域有效质量样本不足")
        }
        return if (goodRate * 100 + 1e-8 < rules.minimumGoodPercent) listOf("目的地区域历史优质报价比例不达标")
            else emptyList()
    }
}
