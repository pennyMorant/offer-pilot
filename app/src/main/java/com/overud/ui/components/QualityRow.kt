package com.overud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable fun QualityRow(state: QualityCardState) {
    var detail by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { detail = true }.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(state.title, style = MaterialTheme.typography.titleMedium)
        Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.goodRate?.let { "优质 $it%" } ?: "样本不足")
            Text("${state.averageMile}/mi · ${state.total} 张")
        }
        if (state.total > 0) Row(Modifier.fillMaxWidth().height(6.dp)) {
            listOf(state.good to MaterialTheme.colorScheme.primary, state.normal to MaterialTheme.colorScheme.secondary,
                state.low to MaterialTheme.colorScheme.tertiary, state.unknown to MaterialTheme.colorScheme.outlineVariant)
                .filter { it.first > 0 }.forEach { (count, color) ->
                    Box(Modifier.weight(count.toFloat() / state.total).fillMaxHeight().background(color))
                }
        }
        Text("优 ${state.good} · 普通 ${state.normal} · 低 ${state.low} · 未知 ${state.unknown}",
            style = MaterialTheme.typography.labelSmall)
    }
    HorizontalDivider()
    if (detail) AlertDialog(onDismissRequest = { detail = false }, title = { Text("报价质量详情") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { QualityCard(state) } },
        confirmButton = { TextButton(onClick = { detail = false }) { Text("关闭") } })
}
