package io.github.pennymorant.offerpilot.analytics

import io.github.pennymorant.offerpilot.model.Platform

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
