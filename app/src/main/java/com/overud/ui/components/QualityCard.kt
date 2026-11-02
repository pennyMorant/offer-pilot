package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QualityCard(state: QualityCardState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.title, style = MaterialTheme.typography.titleLarge)
            Text(state.subtitle, style = MaterialTheme.typography.bodySmall)
            Text("${state.total} 张报价 · 优质 ${state.good} · 普通 ${state.normal} · 低价值 ${state.low}")
            Text("信息不足 ${state.unknown} 张", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("平均报价 ${state.averageAmount}")
            Text("每英里报价均值 ${state.averageMile}/mi · 预估毛时薪均值 ${state.averageHour}/h")
            Text(state.goodRate?.let { "可分类样本中的优质比例 $it%" } ?: "可分类样本少于 10 张，暂不判断区域或时段倾向",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
