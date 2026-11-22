package io.github.pennymorant.offerpilot.data.repository

import io.github.pennymorant.offerpilot.data.database.projection.QualitySummary
import io.github.pennymorant.offerpilot.data.database.projection.ExposureSummary

data class StatisticsSnapshot(
    val totals: List<QualitySummary> = emptyList(),
    val times: List<QualitySummary> = emptyList(),
    val regions: List<QualitySummary> = emptyList(),
    val exposures: List<ExposureSummary> = emptyList(),
    val refreshedAt: Long = 0,
    val revision: Long = 0,
)
