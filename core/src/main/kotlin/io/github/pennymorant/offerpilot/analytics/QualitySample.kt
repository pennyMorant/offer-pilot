package io.github.pennymorant.offerpilot.analytics

enum class OrderQuality { GOOD, NORMAL, LOW, UNKNOWN }

/** Temporary features. This is never persisted as an individual order. */
data class QualitySample(
    val amountCents: Long?,
    val distanceMeters: Long?,
    val durationSeconds: Long?,
    val centsPerMileMilli: Long?,
    val centsPerHourMilli: Long?,
    val quality: OrderQuality,
    val fromOcr: Boolean,
)
