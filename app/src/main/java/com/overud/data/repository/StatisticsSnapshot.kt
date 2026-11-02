package com.overud.data.repository

import com.overud.data.database.projection.QualitySummary

data class StatisticsSnapshot(
    val totals: List<QualitySummary> = emptyList(),
    val times: List<QualitySummary> = emptyList(),
    val regions: List<QualitySummary> = emptyList(),
    val refreshedAt: Long = 0,
    val revision: Long = 0,
)
