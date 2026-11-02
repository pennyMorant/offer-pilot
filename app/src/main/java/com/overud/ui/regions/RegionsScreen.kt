package com.overud.ui.regions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.*

@Composable
fun RegionsScreen(state: AnalysisUiState, onSelection: (AnalysisSelection) -> Unit) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageTitle("KNOW YOUR WORKING AREAS", "地区质量，看长期。", "最近 90 天 · 比较相同时段、不同地区的报价质量。") }
        item { AnalysisFilters(state, onSelection, allowRegion = false) }
        item { Text("当前范围 ${state.observedCount} 张报价", style = MaterialTheme.typography.titleMedium) }
        item { QualityStandardNote() }
        item { EmptyStateCard("收到报价时的位置", "按位置网格统计，未知位置单独归组。数据仅代表个人观测，不代表平台全部订单或真实派单频率。") }
        if (state.cards.isEmpty()) item { EmptyStateCard("地区样本不足", "开始工作并授权定位后，报价会归入相应网格。") }
        items(state.cards, key = { it.id }) { QualityCard(it) }
        item { Text("配送耗时与报价对比", style = MaterialTheme.typography.titleLarge) }
        item { DeliveryMeasurementNote() }
        if (state.deliveries.isEmpty()) item {
            EmptyStateCard("此范围还没有配送观测汇总", "明确送达后累计；未识别到完整起点的配送不会补造耗时。")
        }
        items(state.deliveries, key = { it.id }) { DeliveryCard(it) }
    }
}
