package com.overud.ui.regions

data class RegionsUiState(val regions: List<RegionCardState>, val destinations: List<DestinationSummary>)
data class RegionCardState(
    val key: String,
    val platformLabel: String,
    val offerCount: Int,
    val eligibleCount: Int,
    val coverageMinutes: Long,
    val observedRatePerHour: Double?,
)
data class DestinationSummary(val name: String, val offerCount: Int, val eligibleCount: Int)
