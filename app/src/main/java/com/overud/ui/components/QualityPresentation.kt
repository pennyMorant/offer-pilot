package com.overud.ui.components

import com.overud.analytics.AnalysisBucket
import com.overud.data.database.projection.QualitySummary
import com.overud.model.Platform
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun qualityPresentation(summary: QualitySummary, byRegion: Boolean): QualityCardState {
    val platform = Platform.entries.find { it.name == summary.platform }?.label ?: summary.platform
    val title = if (byRegion) regionLabel(summary.regionKey)
        else "${weekdayLabel(summary.weekday)} ${summary.localHour}:00–${summary.localHour + 1}:00"
    val kind = com.overud.model.OfferService.entries.find { it.name == summary.serviceKind }?.label ?: "类型未确认"
    val standard = if (summary.qualityVersion == 0) "旧数据 · 口径未知" else "质量标准 v${summary.qualityVersion}"
    val classified = summary.goodCount + summary.normalCount + summary.lowCount
    return QualityCardState(
        id = "${summary.platform}|${summary.regionKey}|${summary.weekday}|${summary.localHour}|${summary.zoneId}|${summary.qualityVersion}|${summary.serviceKind}",
        title = title, subtitle = "$platform · $kind · ${summary.zoneId} · $standard",
        total = summary.offerCount, good = summary.goodCount, normal = summary.normalCount,
        low = summary.lowCount, unknown = summary.unknownCount,
        averageAmount = averageMoney(summary.amountSum, summary.amountCount, 1),
        averageMile = averageMoney(summary.perMileMilliSum, summary.perMileCount, 1000),
        averageHour = averageMoney(summary.perHourMilliSum, summary.perHourCount, 1000),
        goodRate = if (classified >= 10) (summary.goodCount * 100.0 / classified).roundToInt() else null,
        metricSamples = "金额样本 ${summary.amountCount} · 里程效率样本 ${summary.perMileCount} · 时间效率样本 ${summary.perHourCount}",
    )
}

fun regionLabel(region: String) = if (region == AnalysisBucket.UNKNOWN_REGION) "地区未知" else "网格 $region"
fun weekdayLabel(day: Int) = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    .getOrNull(day - 1) ?: "全部日期"

private fun averageMoney(sum: Long, count: Long, scale: Long): String =
    if (count > 0) money((sum.toDouble() / count / scale).roundToLong()) else "—"
