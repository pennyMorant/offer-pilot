package com.overud.model

enum class ValueGrade { EXCELLENT, GOOD, BORDERLINE, POOR, UNKNOWN }

enum class EvaluationConfidence { HIGH, MEDIUM, LOW, UNKNOWN }

enum class TimeEstimateBasis { QUOTED, CALIBRATED }

data class ScoreComponents(
    val contributionHourly: Double,
    val contributionPerMile: Double,
    val contributionAmount: Double,
)

/** Aggregate calibration only. Ratio is derived from completed/quoted duration buckets. */
data class DurationCalibration(
    val ratio: Double,
    val sampleCount: Long,
    val scope: RegionalEvidenceScope,
) {
    fun valid() = ratio.isFinite() && ratio in 0.5..3.0 && sampleCount >= MINIMUM_SAMPLES

    companion object { const val MINIMUM_SAMPLES = 20L }
}
