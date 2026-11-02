package com.overud.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.*

@Composable
fun StatisticsScreen(state: AnalysisUiState, onSelection: (AnalysisSelection) -> Unit) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageTitle("QUALITY THROUGH THE DAY", "好时段，数据说话。", "最近 90 天 · 按平台、地区、星期与当地小时比较报价质量。") }
        item { AnalysisFilters(state, onSelection, allowRegion = true) }
        item { Text("当前范围 ${state.observedCount} 张报价", style = MaterialTheme.typography.titleMedium) }
        item { QualityStandardNote() }
        if (state.cards.isEmpty()) item { EmptyStateCard("此范围还没有统计样本", "工作期间识别到的报价会归入统计分组，包括未接的报价。") }
        items(state.cards, key = { it.id }) { QualityCard(it) }
        item { Text("配送耗时与报价对比", style = MaterialTheme.typography.titleLarge) }
        item { DeliveryMeasurementNote() }
        if (state.deliveries.isEmpty()) item {
            EmptyStateCard("此范围还没有配送观测汇总", "明确送达后累计；未识别到完整起点的配送不会补造耗时。")
        }
        items(state.deliveries, key = { it.id }) { DeliveryCard(it) }
    }
}
