package com.overud.ui.components

import com.overud.data.database.projection.QualitySummary
import com.overud.model.Platform

enum class AnalysisPeriod(val label: String, val hours: Set<Int>) {
    ALL("全天", (0..23).toSet()),
    BREAKFAST("早餐", (6..10).toSet()),
    LUNCH("午餐", (11..14).toSet()),
    AFTERNOON("下午", (15..16).toSet()),
    DINNER("晚餐", (17..21).toSet()),
    NIGHT("夜间", (22..23).toSet() + (0..5).toSet());

    val rangeLabel: String get() = when (this) {
        ALL -> "00:00–24:00"
        BREAKFAST -> "06:00–11:00"
        LUNCH -> "11:00–15:00"
        AFTERNOON -> "15:00–17:00"
        DINNER -> "17:00–22:00"
        NIGHT -> "当天 00:00–06:00 及 22:00–24:00"
    }
}

data class AnalysisSelection(
    val platform: Platform? = null,
    val serviceKind: com.overud.model.OfferService? = null,
    val region: String? = null,
    val weekday: Int? = null,
    val period: AnalysisPeriod = AnalysisPeriod.ALL,
) {
    fun matches(row: QualitySummary): Boolean =
        (serviceKind == null || row.serviceKind == serviceKind.name) &&
        (platform == null || row.platform == platform.name) &&
        (region == null || row.regionKey == region) &&
        (weekday == null || row.weekday == weekday) && row.localHour in period.hours
}
