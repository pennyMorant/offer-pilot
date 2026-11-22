package io.github.pennymorant.offerpilot.ui.components

import io.github.pennymorant.offerpilot.model.EvaluationConfidence
import io.github.pennymorant.offerpilot.model.TimeEstimateBasis
import io.github.pennymorant.offerpilot.model.Valuation
import io.github.pennymorant.offerpilot.model.ValueGrade
import java.util.Locale

data class ValueRatingUiState(
    val score: String,
    val grade: String,
    val confidence: String,
    val hourly: String,
    val perMile: String,
    val timeBasis: String,
    val breakdown: String,
) {
    val compact: String
        get() = "价值分 $score · $grade · $confidence\n扣成本后 $hourly/h · $perMile/mi"
}

fun valueRatingPresentation(value: Valuation?): ValueRatingUiState = ValueRatingUiState(
    score = value?.score?.toString() ?: "—",
    grade = value?.grade.label(),
    confidence = value?.confidence.label(),
    hourly = estimatedMoney(value?.contributionCentsPerHour),
    perMile = estimatedMoney(value?.contributionCentsPerMile),
    timeBasis = value?.timeBasis.label(),
    breakdown = value?.scoreComponents?.let {
        "时薪 ${it.contributionHourly.toInt()} · 每英里 ${it.contributionPerMile.toInt()} · 单笔 ${it.contributionAmount.toInt()}"
    } ?: "评分分项待确认",
)

private fun ValueGrade?.label() = when (this) {
    ValueGrade.EXCELLENT -> "优秀"
    ValueGrade.GOOD -> "合格"
    ValueGrade.BORDERLINE -> "临界"
    ValueGrade.POOR -> "较差"
    ValueGrade.UNKNOWN, null -> "待分析"
}

private fun EvaluationConfidence?.label() = when (this) {
    EvaluationConfidence.HIGH -> "高可信"
    EvaluationConfidence.MEDIUM -> "中可信"
    EvaluationConfidence.LOW -> "低可信"
    EvaluationConfidence.UNKNOWN, null -> "可信度未知"
}

private fun TimeEstimateBasis?.label() = when (this) {
    TimeEstimateBasis.CALIBRATED -> "历史聚合校准耗时"
    TimeEstimateBasis.QUOTED -> "平台预计耗时"
    null -> "耗时未知"
}

internal fun estimatedMoney(cents: Double?): String =
    if (cents != null && cents.isFinite()) String.format(Locale.US, "$%.2f", cents / 100) else "—"
