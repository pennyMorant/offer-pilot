package com.overud.ui.components

import com.overud.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DestinationUiState(
    val title: String, val detail: String, val evidence: String?, val timing: String?,
    val waiting: String? = null,
)

fun destinationPresentation(assessment: DestinationAssessment, locale: Locale): DestinationUiState {
    val resolution = assessment.resolution
    if (resolution != DestinationResolution.RESOLVED) {
        val (title, detail) = when (resolution) {
            DestinationResolution.MISSING -> "目的地未识别" to "需要明确的送达地址或坐标，地图文字不足时保留未知。"
            DestinationResolution.RESOLVING -> "正在分析目的地区域" to "地址解析在后台进行，当前报价计算与接单不等待它。"
            DestinationResolution.UNAVAILABLE -> "目的地区域暂不可用" to "系统地址解析未返回有效结果。"
            DestinationResolution.AMBIGUOUS -> "目的地存在多个候选" to "无法确定送达网格，暂不使用区域判断。"
            DestinationResolution.TOO_BROAD -> "目的地位置不够明确" to "只识别到城市或宽泛区域，无法可靠映射到网格。"
            DestinationResolution.RESOLVED -> error("Handled separately")
        }
        return DestinationUiState(title, detail, null, null)
    }
    val title = when (assessment.outlook) {
        RegionalOutlook.GOOD -> "目的地历史报价质量较好"
        RegionalOutlook.MIXED -> "目的地历史报价质量一般"
        RegionalOutlook.WEAK -> "目的地低价值报价偏多"
        RegionalOutlook.INSUFFICIENT -> "目的地区域样本不足"
    }
    val precision = when (assessment.precision) {
        DestinationPrecision.COORDINATE -> "坐标定位"
        DestinationPrecision.ADDRESS -> "地址估计"
        DestinationPrecision.STREET -> "街道估计"
        null -> "位置精度未知"
    }
    val scope = when (assessment.evidenceScope) {
        RegionalEvidenceScope.SAME_HOUR -> "同地区、同星期和送达小时"
        RegionalEvidenceScope.ADJACENT_HOURS -> "同地区、送达前后一小时"
        RegionalEvidenceScope.SAME_HOUR_OTHER_DAYS -> "同地区、各星期的同一小时"
        RegionalEvidenceScope.NEIGHBORHOOD -> "周边网格、同星期和送达小时"
        RegionalEvidenceScope.REGION_BASELINE -> "该地区各时段的整体参考"
        RegionalEvidenceScope.NONE -> "尚无对应样本"
    }
    val share = assessment.goodRate?.let { " · 优质比例 ${"%.0f".format(locale, it * 100)}%" }.orEmpty()
    val evidence = "$scope · ${assessment.observedCount} 张报价，${assessment.classifiedCount} 张可分类$share"
    val zoneId = assessment.analysisZoneId ?: TimeZone.getDefault().id
    val timing = assessment.arrivalAt?.let {
        val format = SimpleDateFormat("EEE HH:mm", locale).apply {
            timeZone = TimeZone.getTimeZone(zoneId)
        }
        "预计送达 ${format.format(Date(it))} · 按手机时区 $zoneId 分析"
    } ?: "总耗时未知，仅展示地区整体参考"
    val region = assessment.regionKey?.let(::regionLabel) ?: "地区未知"
    return DestinationUiState(title, "$precision · $region", evidence, timing, waitingPresentation(assessment.waiting, locale))
}
