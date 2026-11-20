package com.overud.ui.components

import com.overud.analytics.AnalysisBucket
import com.overud.analytics.QualityEvaluator
import com.overud.data.database.projection.QualitySummary
import com.overud.model.OfferService
import com.overud.model.Platform
import com.overud.region.RegionCell
import java.util.Locale
import kotlin.math.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun opportunityPresentation(summary: QualitySummary, exposureMs: Long, currentRegion: String?): OpportunityCardState {
    val classified = summary.goodCount + summary.normalCount + summary.lowCount
    val hours = exposureMs / 3_600_000.0
    val goodRate = if (classified >= 10) summary.goodCount.toDouble() / classified else null
    val offerRate = if (hours >= 1.0) summary.offerCount / hours else null
    val mileCents = average(summary.perMileMilliSum, summary.perMileCount)
    val hourCents = average(summary.perHourMilliSum, summary.perHourCount)
    val ready = summary.regionKey != AnalysisBucket.UNKNOWN_REGION &&
        summary.qualityVersion == QualityEvaluator.VERSION && classified >= 10 &&
        summary.perMileCount >= 10 && summary.perHourCount >= 10 && offerRate != null
    val score = if (ready && goodRate != null && mileCents != null && hourCents != null) {
        opportunityScore(goodRate, offerRate, mileCents, hourCents)
    } else null
    val confidence = when {
        !ready -> "证据不足"
        classified >= 50 && hours >= 10 -> "高可信"
        classified >= 25 && hours >= 5 -> "中可信"
        else -> "初步参考"
    }
    val platform = Platform.entries.find { it.name == summary.platform }?.label ?: summary.platform
    val service = OfferService.entries.find { it.name == summary.serviceKind }?.label ?: "类型未确认"
    val distance = cellDistanceMiles(currentRegion, summary.regionKey)
    val proximity = when {
        currentRegion == summary.regionKey -> "当前区域"
        distance != null -> String.format(Locale.getDefault(), "距当前约 %.1f mi（直线）", distance)
        else -> "当前位置未知"
    }
    return OpportunityCardState(
        id = "opportunity|${summary.platform}|${summary.serviceKind}|${summary.regionKey}|" +
            "${summary.zoneId}|${summary.qualityVersion}",
        title = regionLabel(summary.regionKey),
        subtitle = "$platform · $service · 历史区域机会",
        score = score,
        goodRate = goodRate?.times(100)?.roundToInt(),
        offersPerHour = offerRate,
        averageMile = mileCents?.roundToLong()?.let(::money) ?: "—",
        averageHour = hourCents?.roundToLong()?.let(::money) ?: "—",
        samples = "报价 ${summary.offerCount} 张 · 有效质量 $classified · 观察 ${hoursText(hours)} 小时",
        confidence = confidence,
        advice = advice(score),
        knownRegion = summary.regionKey != AnalysisBucket.UNKNOWN_REGION,
        proximity = proximity,
        distanceMiles = distance,
    )
}

private fun opportunityScore(goodRate: Double, offersPerHour: Double, mile: Double, hour: Double): Int {
    val quality = goodRate.coerceIn(0.0, 1.0) * 45
    val availability = (offersPerHour / 3.0).coerceIn(0.0, 1.0) * 30
    val efficiency = ((mile / 200).coerceIn(0.0, 1.0) +
        (hour / 2500).coerceIn(0.0, 1.0)) / 2 * 25
    return (quality + availability + efficiency).roundToInt().coerceIn(0, 100)
}

private fun advice(score: Int?) = when {
    score == null -> "样本或观察时长不足，暂不参与区域排序"
    score >= 75 -> "历史质量、出现频率和报价效率综合较好，可作为候选等待区域"
    score >= 60 -> "历史综合表现一般，建议结合当前距离和平台状态判断"
    else -> "历史综合效率偏低，不建议仅因曾出现高价订单前往"
}

private fun average(sum: Long, count: Long): Double? =
    if (count > 0) sum.toDouble() / count / 1000 else null

private fun hoursText(value: Double) = String.format(Locale.getDefault(), "%.1f", value)

private fun cellDistanceMiles(current: String?, target: String): Double? {
    val first = current?.let(RegionCell::parse) ?: return null
    val second = RegionCell.parse(target) ?: return null
    val lat1 = Math.toRadians(first.centerLatitude)
    val lat2 = Math.toRadians(second.centerLatitude)
    val deltaLat = lat2 - lat1
    val deltaLon = Math.toRadians(second.centerLongitude - first.centerLongitude)
    val a = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    return 3958.8 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
