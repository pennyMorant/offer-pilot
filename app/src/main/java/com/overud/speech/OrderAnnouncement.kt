package com.overud.speech

import com.overud.model.*
import com.overud.work.WorkState
import java.util.Locale

object OrderAnnouncement {
    fun text(state: WorkState, includeProp22: Boolean): String? {
        val offer = state.latest ?: return null
        val decision = state.decision ?: return null
        val amount = offer.amountCents ?: return null
        if (offer.currency != "USD" || amount <= 0) return null
        if (!offer.reliable && offer.evidence?.amountVerified != true) return null
        return buildString {
            append("${offer.platform.label}，报价${money(amount / 100.0)}美元。")
            if (offer.hasVerifiedDistance()) offer.distanceMeters?.let {
                append("${decimal(it / 1609.344)}英里，")
            } else append("总里程未确认，")
            if (offer.hasVerifiedDuration()) offer.durationSeconds?.let {
                append("预计${decimal(it / 60.0)}分钟。")
            } else append("总耗时未确认。")
            decision.valuation.score?.let {
                append("价值评分${it}分，${grade(decision.valuation.grade)}，" +
                    "${confidence(decision.valuation.confidence)}。")
            }
            append(if (decision.eligible) "符合接单要求，不代表已接单。" else
                "未通过接单判断，${decision.reasons.firstOrNull().orEmpty()}。")
            if (includeProp22) decision.valuation.prop22?.extraCents?.let {
                append("按设置假设，二十二号法案情景补差${money(it / 100.0)}美元，实际可能为零。")
            }
        }
    }

    private fun decimal(value: Double) = String.format(Locale.US, "%.1f", value)
    private fun money(value: Double) = String.format(Locale.US, "%.2f", value)

    private fun grade(value: ValueGrade) = when (value) {
        ValueGrade.EXCELLENT -> "优秀"
        ValueGrade.GOOD -> "合格"
        ValueGrade.BORDERLINE -> "临界"
        ValueGrade.POOR -> "较差"
        ValueGrade.UNKNOWN -> "待分析"
    }

    private fun confidence(value: EvaluationConfidence) = when (value) {
        EvaluationConfidence.HIGH -> "高可信"
        EvaluationConfidence.MEDIUM -> "中可信"
        EvaluationConfidence.LOW -> "低可信"
        EvaluationConfidence.UNKNOWN -> "可信度未知"
    }
}
