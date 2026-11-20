package com.overud.model

data class Valuation(
    val centsPerMile: Double?,
    val grossCentsPerHour: Double?,
    val contributionCents: Double?,
    val vehicleCostCents: Double? = null,
    val contributionCentsPerMile: Double? = null,
    val contributionCentsPerHour: Double? = null,
    val effectiveDurationSeconds: Long? = null,
    val timeBasis: TimeEstimateBasis = TimeEstimateBasis.QUOTED,
    val score: Int? = null,
    val grade: ValueGrade = ValueGrade.UNKNOWN,
    val confidence: EvaluationConfidence = EvaluationConfidence.UNKNOWN,
    val scoreComponents: ScoreComponents? = null,
    val prop22: Prop22Estimate? = null,
    val scoreNotes: List<String> = emptyList(),
    val deadheadAdjusted: Boolean = false,
)
data class Decision(val eligible: Boolean, val reasons: List<String>, val valuation: Valuation, val policyVersion: Long)
