package com.overud.analytics

import com.overud.model.Platform

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
    val serviceKind: com.overud.model.OfferService = com.overud.model.OfferService.UNKNOWN,
) {
    val classified get() = good + normal + low
}
