package com.overud.ui.components

data class QualityCardState(
    val id: String,
    val title: String,
    val subtitle: String,
    val total: Long,
    val good: Long,
    val normal: Long,
    val low: Long,
    val unknown: Long,
    val averageAmount: String,
    val averageMile: String,
    val averageHour: String,
    val goodRate: Int?,
    val mileValue: Double = 0.0,
    val mileSamples: Long = 0,
    val metricSamples: String,
)
