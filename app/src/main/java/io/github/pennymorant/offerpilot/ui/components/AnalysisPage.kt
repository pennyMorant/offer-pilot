@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
        "附近优先" -> state.opportunities.sortedBy { it.distanceMiles ?: Double.MAX_VALUE }
        else -> state.opportunities.sortedByDescending { it.score ?: -1 }
    }
    val ranked = opportunities.filter { it.knownRegion && it.score != null }
    val unranked = opportunities.filterNot { it.knownRegion && it.score != null }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        checked = !delivery,
                        onCheckedChange = { delivery = false },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(if (byRegion) "机会分析" else "报价质量", style = MaterialTheme.typography.labelLarge) }
                    ToggleButton(
                        checked = delivery,
                        onCheckedChange = { delivery = true },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text("配送表现", style = MaterialTheme.typography.labelLarge) }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(listOf(state.selection.platform?.label ?: "全部平台", state.selection.serviceKind?.label ?: "全部类型",
                            state.selection.period.label, state.selection.weekday?.let(::weekdayLabel) ?: "全部星期",
                            state.selection.region?.let(::regionLabel) ?: "全部地区").joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = { filters = true }, shapes = ButtonDefaults.shapes(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("筛选范围", style = MaterialTheme.typography.labelLarge)
                            }
                            TextButton(onClick = { explanation = true }, shapes = ButtonDefaults.shapes()) {
                                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("统计口径", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        if (!delivery) {
            item { AnalysisOverview(state) }
            if (byRegion) item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("区域机会", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("机会指数", "报价质量", "报价频率", "附近优先").forEach { label ->
                            FilterChip(sort == label, onClick = { sort = label }, label = { Text(label) })
                        }
                    }
                    Text("综合历史优质比例、每观察小时报价量及毛报价效率；证据不足和未知地区列在末尾。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else item { Text("时段对比", style = MaterialTheme.typography.titleMedium) }
            if (byRegion) {
                if (state.opportunities.isEmpty()) item { AnalysisEmptyHint("当前范围暂无区域样本，工作期间会自动累计。") }
                items(ranked + unranked, key = { it.id }) { OpportunityRow(it) }
            } else {
                if (state.cards.isEmpty()) item { AnalysisEmptyHint("当前范围暂无报价样本，工作期间会自动累计。") }
                items(state.cards, key = { it.id }) { QualityRow(it) }
            }
        } else {
            item { Text("配送耗时与报价对比", style = MaterialTheme.typography.titleMedium) }
            if (state.deliveries.isEmpty()) item { AnalysisEmptyHint("暂无完整配送观测，明确送达后会累计。") }
            items(state.deliveries, key = { it.id }) { DeliveryCard(it) }
        }
    }
    if (filters) ModalBottomSheet(onDismissRequest = { filters = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("筛选范围", style = MaterialTheme.typography.titleLarge)
            AnalysisFilters(state, onSelection, allowRegion = !byRegion)
            Button(onClick = { filters = false }, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("查看结果", style = MaterialTheme.typography.titleMedium) }
        }
    }
    if (explanation) AlertDialog(onDismissRequest = { explanation = false }, title = { Text("统计口径") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            if (delivery) DeliveryMeasurementNote()
            else if (byRegion) OpportunityStandardNote()
            else QualityStandardNote()
            if (byRegion) Text("按收到报价时的位置网格累计，未知位置单独分组。")
        } }, confirmButton = { TextButton(onClick = { explanation = false }, shapes = ButtonDefaults.shapes()) { Text("知道了") } })
}

@Composable private fun AnalysisEmptyHint(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp))
    }
}
