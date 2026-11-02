package com.overud.ui.components

import com.overud.analytics.QualitySummaryReducer
import com.overud.data.repository.StatisticsSnapshot
import com.overud.data.database.projection.DeliverySummary

data class AnalysisUiState(
    val selection: AnalysisSelection = AnalysisSelection(),
    val availableRegions: List<String> = emptyList(),
    val cards: List<QualityCardState> = emptyList(),
    val observedCount: Long = 0,
    val deliveries: List<DeliveryCardState> = emptyList(),
)

fun analysisState(
    snapshot: StatisticsSnapshot, selection: AnalysisSelection, byRegion: Boolean,
    deliveryHistory: List<DeliverySummary> = emptyList(),
): AnalysisUiState {
    val rows = snapshot.times.filter(selection::matches)
    val groups = QualitySummaryReducer.group(rows, byRegion)
    val sorted = if (byRegion) groups.sortedByDescending { it.offerCount }
        else groups.sortedWith(compareBy({ it.weekday }, { it.localHour }, { it.platform }, { it.zoneId }, { it.qualityVersion }))
    return AnalysisUiState(selection, (snapshot.regions.map { it.regionKey } + deliveryHistory.map { it.regionKey }).distinct().sorted(),
        sorted.map { qualityPresentation(it, byRegion) }, rows.sumOf { it.offerCount },
        deliveryCards(deliveryHistory, selection, byRegion))
}
