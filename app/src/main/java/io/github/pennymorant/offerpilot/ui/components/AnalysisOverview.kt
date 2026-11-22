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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${state.observedCount} 张报价", style = MaterialTheme.typography.headlineMedium)
        Text("最近 90 天 · 当前筛选范围", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f)) {
                Text("优质占比", style = MaterialTheme.typography.labelMedium)
                Text(if (summary.classified >= 10) "${(summary.good * 100.0 / summary.classified).roundToLong()}%" else "样本不足",
                    style = MaterialTheme.typography.titleLarge)
                Text("有效样本 ${summary.classified}", style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text("每英里报价均值", style = MaterialTheme.typography.labelMedium)
                Text(if (summary.mileCount > 0) money((summary.mileSum.toDouble() / summary.mileCount / 1000).roundToLong()) else "—",
                    style = MaterialTheme.typography.titleLarge)
                Text("有效样本 ${summary.mileCount}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
