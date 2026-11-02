package com.overud.model

enum class Prop22Status { ESTIMATED, INVALID_SETTINGS, RATE_YEAR_UNSUPPORTED, UNSUPPORTED_OFFER, MISSING_ROUTE, MISSING_AMOUNT }

data class Prop22Estimate(
    val status: Prop22Status,
    val minimumWageCents: Long,
    val assumedTipPercent: Int,
    val timeFloorCents: Double? = null,
    val mileageFloorCents: Double? = null,
    val floorCents: Double? = null,
    val assumedPlatformPayCents: Double? = null,
    val assumedTipCents: Double? = null,
    val extraCents: Double? = null,
    val totalCents: Double? = null,
)
