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
    var sort by rememberSaveable { mutableStateOf("样本量") }
    val cards = if (!byRegion) state.cards else when (sort) {
        "报价质量" -> state.cards.sortedByDescending { it.goodRate ?: -1 }
        "里程效率" -> state.cards.sortedByDescending { if (it.mileSamples >= 10) it.mileValue else -1.0 }
        else -> state.cards.sortedByDescending { it.total }
    }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!delivery, onClick = { delivery = false }, label = { Text("报价质量") })
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
                Text("收到报价所在地的历史质量", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("样本量", "报价质量", "里程效率").forEach { label ->
                        FilterChip(sort == label, onClick = { sort = label }, label = { Text(label) })
                    }
                }
                Text("不足 10 个有效样本的指标不参与质量或效率排名。", style = MaterialTheme.typography.bodySmall)
            } else item { Text("时段对比", style = MaterialTheme.typography.titleMedium) }
            if (cards.isEmpty()) item { Text("当前范围暂无报价样本。工作期间识别的报价会自动累计。") }
            items(cards, key = { it.id }) { QualityRow(it) }
        } else {
            item { Text("配送耗时与报价对比", style = MaterialTheme.typography.titleMedium) }
            if (state.selection.serviceKind != null) item { Text("配送观测尚未区分订单类型，请将类型筛选设为“全部类型”。") }
            else {
                if (state.deliveries.isEmpty()) item { Text("暂无完整配送观测，明确送达后会累计。") }
                items(state.deliveries, key = { it.id }) { DeliveryCard(it) }
            }
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
            if (delivery) DeliveryMeasurementNote() else QualityStandardNote()
            if (byRegion) Text("按收到报价时的位置网格累计，未知位置单独分组。个人历史报价质量不代表下一单概率或实际等待预测。")
        } }, confirmButton = { TextButton(onClick = { explanation = false }) { Text("知道了") } })
}
