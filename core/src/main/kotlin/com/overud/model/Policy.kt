package com.overud.model

data class Policy(
    val version: Long = 1,
    val enabledPlatforms: Set<Platform> = Platform.entries.toSet(),
    val minimumCents: Long = 800,
    val minimumCentsPerMile: Long = 200,
    val minimumCentsPerHour: Long = 2500,
    val maximumMeters: Long = 16093,
    val maximumSeconds: Long = 2700,
    val maximumPickupMeters: Long? = null,
    val costCentsPerMile: Long = 35,
    val allowStacked: Boolean = false,
    val allowAddon: Boolean = false,
    val excludedDestinations: Set<String> = emptySet(),
) {
    fun valid() = minimumCents > 0 && minimumCentsPerMile > 0 && minimumCentsPerHour > 0 &&
        maximumMeters > 0 && maximumSeconds > 0 && costCentsPerMile >= 0 &&
        (maximumPickupMeters == null || maximumPickupMeters > 0) && enabledPlatforms.isNotEmpty()
}
