package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

data class AnalysisOverview(
    val classified: Long = 0, val good: Long = 0, val mileCount: Long = 0, val mileSum: Long = 0,
)

@Composable fun AnalysisOverview(state: AnalysisUiState) {
    val summary = state.overview
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${state.observedCount} 张报价",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    "最近 90 天 · 当前筛选范围",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolMetricTile(
                    label = "优质占比（样本 ${summary.classified}）",
                    value = if (summary.classified >= 10) "${(summary.good * 100.0 / summary.classified).roundToLong()}%" else "样本不足",
                    modifier = Modifier.weight(1f),
                    highlight = summary.classified >= 10,
                )
                ToolMetricTile(
                    label = "每英里均值（样本 ${summary.mileCount}）",
                    value = if (summary.mileCount > 0) money((summary.mileSum.toDouble() / summary.mileCount / 1000).roundToLong()) else "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
