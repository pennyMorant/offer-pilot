package com.overud.analytics

import com.overud.model.Platform

data class WaitingObservation(
    val platform: Platform,
    val region: String,
    val zoneId: String,
    val weekday: Int,
    val hour: Int,
    val version: Int,
    val completed: Long,
    val interrupted: Long,
    val completedMillis: Long,
)
