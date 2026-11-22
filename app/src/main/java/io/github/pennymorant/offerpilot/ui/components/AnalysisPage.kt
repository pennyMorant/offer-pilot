@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val sortOptions = listOf("机会指数" to "指数", "报价质量" to "质量", "报价频率" to "频率", "附近优先" to "附近")

@Composable
fun AnalysisPage(state: AnalysisUiState, onSelection: (AnalysisSelection) -> Unit, byRegion: Boolean) {
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
    val hasCustomFilter = state.selection.platform != null || state.selection.serviceKind != null ||
        state.selection.weekday != null || state.selection.region != null
    val filterSummary = listOfNotNull(
        state.selection.platform?.label ?: "全部",
        state.selection.period.label,
        state.selection.weekday?.let(::weekdayLabel),
    ).joinToString("·")

    LazyColumn(
        Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AnalysisToolbar(
                byRegion = byRegion, delivery = delivery, hasCustomFilter = hasCustomFilter,
                filterSummary = filterSummary, onDelivery = { delivery = it },
                onFilter = { filters = true }, onInfo = { explanation = true },
            )
        }
        if (!delivery) {
            if (byRegion) {
                ranked.firstOrNull()?.let { top -> item { OpportunityHeroCard(top) } }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        sortOptions.forEachIndexed { idx, (key, short) ->
                            val shapes = when (idx) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                sortOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            ToggleButton(
                                checked = sort == key, onCheckedChange = { sort = key },
                                shapes = shapes, modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            ) { Text(short, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        }
                    }
                }
                if (state.opportunities.isEmpty()) {
                    item { AnalysisEmptyHint("当前范围暂无区域样本，开启工作后会自动累计。") }
                } else {
                    val restRanked = if (ranked.size > 1) ranked.drop(1) else emptyList()
                    if (restRanked.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                restRanked.forEachIndexed { idx, op -> OpportunityRow(op, idx, restRanked.size) }
                            }
                        }
                    }
                    if (unranked.isNotEmpty()) {
                        item {
                            Text("样本积累中 · 暂未排名", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                unranked.forEachIndexed { idx, op -> OpportunityRow(op, idx, unranked.size, muted = true) }
                            }
                        }
                    }
                }
            } else {
                item { AnalysisOverview(state) }
                if (state.cards.isEmpty()) {
                    item { AnalysisEmptyHint("当前范围暂无报价样本，开启工作后会自动累计。") }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.cards.forEachIndexed { idx, card -> QualityRow(card, idx, state.cards.size) }
                        }
                    }
                }
            }
        } else {
            if (state.deliveries.isEmpty()) {
                item { AnalysisEmptyHint("暂无完整配送观测，明确送达后会累计。") }
            } else {
                itemsIndexed(state.deliveries, key = { _, it -> it.id }) { idx, d ->
                    DeliveryCard(d, idx, state.deliveries.size)
                }
            }
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
            if (delivery) DeliveryMeasurementNote() else if (byRegion) OpportunityStandardNote() else QualityStandardNote()
            if (byRegion) Text("按收到报价时的位置网格累计，未知位置单独分组。")
        } }, confirmButton = { TextButton(onClick = { explanation = false }, shapes = ButtonDefaults.shapes()) { Text("知道了") } })
}

@Composable
private fun AnalysisEmptyHint(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp))
    }
}
