package io.github.pennymorant.offerpilot.ui.components

import io.github.pennymorant.offerpilot.model.*
import java.util.Locale

fun waitingPresentation(assessment: WaitingAssessment?, locale: Locale): String? {
    assessment ?: return null
    val samples = "接到报价 ${assessment.completedCount} 次 · 观测中断 ${assessment.interruptedCount} 次"
    if (assessment.outlook == WaitingOutlook.INSUFFICIENT) {
        return "送达后等待证据不足（样本少或中断过多） · $samples"
    }
    val scope = when (assessment.scope) {
        RegionalEvidenceScope.SAME_HOUR -> "同星期、送达小时"
        RegionalEvidenceScope.ADJACENT_HOURS -> "送达前后一小时"
        RegionalEvidenceScope.SAME_HOUR_OTHER_DAYS -> "各星期同一小时"
        RegionalEvidenceScope.REGION_BASELINE -> "该地区各时段"
        else -> "该地区"
    }
    val mean = assessment.completedMeanSeconds ?: return "送达后等待证据不足 · $samples"
    val minutes = String.format(locale, "%.1f", mean / 60)
    return "$scope · 完成样本平均观测等待 $minutes 分钟 · $samples。" +
        "仅统计送达后持续观察同平台页面、30 分钟内出现下一报价的样本；中断另计。"
}
