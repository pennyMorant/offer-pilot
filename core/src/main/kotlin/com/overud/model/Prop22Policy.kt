package com.overud.model

/** User confirms California applicability and the pickup locality's wage; not geofenced. */
data class Prop22Policy(
    val enabled: Boolean = false,
    val minimumWageCents: Long = 1690,
    val motorVehicle: Boolean = true,
    val assumedTipPercent: Int = 0,
) {
    fun valid() = minimumWageCents in 1690..100_000 && assumedTipPercent in 0..100
}
