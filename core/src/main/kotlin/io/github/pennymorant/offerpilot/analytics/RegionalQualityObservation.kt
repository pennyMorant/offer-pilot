package io.github.pennymorant.offerpilot.analytics

import io.github.pennymorant.offerpilot.model.Platform

data class RegionalQualityObservation(
    val platform: Platform,
    val region: String,
    val zoneId: String,
    val weekday: Int,
    val hour: Int,
    val version: Int,
    val observed: Long,
    val good: Long,
    val normal: Long,
    val low: Long,
    val serviceKind: io.github.pennymorant.offerpilot.model.OfferService = io.github.pennymorant.offerpilot.model.OfferService.UNKNOWN,
) {
    val classified get() = good + normal + low
}
