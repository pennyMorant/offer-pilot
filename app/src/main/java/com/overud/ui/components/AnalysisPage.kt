@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared controls and details, with separate time and region comparison purposes. */
@Composable fun AnalysisPage(state: AnalysisUiState, onSelection: (AnalysisSelection) -> Unit, byRegion: Boolean) {
    var filters by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf(false) }
    var delivery by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf("机会指数") }
    val opportunities = when (sort) {
        "报价质量" -> state.opportunities.sortedByDescending { it.goodRate ?: -1 }
        "报价频率" -> state.opportunities.sortedByDescending { it.offersPerHour ?: -1.0 }
        else -> state.opportunities.sortedByDescending { it.score ?: -1 }
    }
    val ranked = opportunities.filter { it.knownRegion && it.score != null }
    val unranked = opportunities.filterNot { it.knownRegion && it.score != null }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!delivery, onClick = { delivery = false },
                    label = { Text(if (byRegion) "机会分析" else "报价质量") })
                FilterChip(delivery, onClick = { delivery = true }, label = { Text("配送表现") })
            }
            Text(listOf(state.selection.platform?.label ?: "全部平台", state.selection.serviceKind?.label ?: "全部类型",
                state.selection.period.label, state.selection.weekday?.let(::weekdayLabel) ?: "全部星期",
                state.selection.region?.let(::regionLabel) ?: "全部地区").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick = { filters = true }) { Text("筛选") }
                TextButton(onClick = { explanation = true }) { Text("统计口径") }
            }
        }
        if (!delivery) {
            item { AnalysisOverview(state) }
            if (byRegion) item {
                Text("区域机会", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("机会指数", "报价质量", "报价频率").forEach { label ->
                        FilterChip(sort == label, onClick = { sort = label }, label = { Text(label) })
                    }
                }
                Text("综合历史优质比例、每观察小时报价量及毛报价效率；证据不足和未知地区列在末尾。",
                    style = MaterialTheme.typography.bodySmall)
            } else item { Text("时段对比", style = MaterialTheme.typography.titleMedium) }
            if (byRegion) {
                if (state.opportunities.isEmpty()) item { Text("当前范围暂无区域样本。工作期间会自动累计。") }
                items(ranked + unranked, key = { it.id }) { OpportunityRow(it) }
            } else {
                if (state.cards.isEmpty()) item { Text("当前范围暂无报价样本。工作期间会自动累计。") }
                items(state.cards, key = { it.id }) { QualityRow(it) }
            }
        } else {
            item { Text("配送耗时与报价对比", style = MaterialTheme.typography.titleMedium) }
            if (state.deliveries.isEmpty()) item { Text("暂无完整配送观测，明确送达后会累计。") }
            items(state.deliveries, key = { it.id }) { DeliveryCard(it) }
        }
    }
    if (filters) ModalBottomSheet(onDismissRequest = { filters = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("筛选范围", style = MaterialTheme.typography.titleLarge)
            AnalysisFilters(state, onSelection, allowRegion = !byRegion)
            Button(onClick = { filters = false }, modifier = Modifier.fillMaxWidth()) { Text("查看结果") }
        }
    }
    if (explanation) AlertDialog(onDismissRequest = { explanation = false }, title = { Text("统计口径") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            if (delivery) DeliveryMeasurementNote()
            else if (byRegion) OpportunityStandardNote()
            else QualityStandardNote()
            if (byRegion) Text("按收到报价时的位置网格累计，未知位置单独分组。")
        } }, confirmButton = { TextButton(onClick = { explanation = false }) { Text("知道了") } })
}
