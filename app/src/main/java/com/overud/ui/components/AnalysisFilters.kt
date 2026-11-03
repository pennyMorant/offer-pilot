package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.model.Platform

@Composable
fun AnalysisFilters(state: AnalysisUiState, onChange: (AnalysisSelection) -> Unit, allowRegion: Boolean) {
    val selection = state.selection
    var showRegions by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("平台", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selection.platform == null,
                onClick = { onChange(selection.copy(platform = null)) }, label = { Text("全部") })
            Platform.entries.forEach { platform ->
                FilterChip(selected = selection.platform == platform,
                    onClick = { onChange(selection.copy(platform = platform)) }, label = { Text(platform.label) })
            }
        }
        Text("订单类型", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selection.serviceKind == null, onClick = { onChange(selection.copy(serviceKind = null)) }, label = { Text("全部") })
            com.overud.model.OfferService.entries.forEach { kind ->
                FilterChip(selection.serviceKind == kind, onClick = { onChange(selection.copy(serviceKind = kind)) }, label = { Text(kind.label) })
            }
        }
        Text("星期", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selection.weekday == null,
                onClick = { onChange(selection.copy(weekday = null)) }, label = { Text("全部") })
            (1..7).forEach { day ->
                FilterChip(selected = selection.weekday == day,
                    onClick = { onChange(selection.copy(weekday = day)) }, label = { Text(weekdayLabel(day)) })
            }
        }
        Text("时段", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalysisPeriod.entries.forEach { period ->
                FilterChip(selected = selection.period == period,
                    onClick = { onChange(selection.copy(period = period)) }, label = { Text(period.label) })
            }
        }
        Text("当地时间：${selection.period.rangeLabel}", style = MaterialTheme.typography.bodySmall)
        if (allowRegion) {
            OutlinedButton(onClick = { showRegions = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selection.region?.let(::regionLabel) ?: "全部地区")
            }
        }
    }
    if (showRegions) RegionPicker(state.availableRegions,
        onSelect = { onChange(selection.copy(region = it)); showRegions = false },
        onDismiss = { showRegions = false })
}
