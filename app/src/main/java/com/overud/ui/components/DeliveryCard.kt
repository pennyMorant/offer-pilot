package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeliveryCard(state: DeliveryCardState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.title, style = MaterialTheme.typography.titleMedium)
            Text(state.subtitle, style = MaterialTheme.typography.labelMedium)
            Text(state.samples, style = MaterialTheme.typography.bodySmall)
            Text(state.duration, style = MaterialTheme.typography.bodyLarge)
            Text(state.comparison, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DeliveryMeasurementNote() {
    Text("配送耗时观测 · 按观测起点地区和小时归组。完整计时从本次自动接单动作开始，" +
        "到明确送达界面，包含页面确认延迟；手动配送、重启或中断后起点不明时仅计完成次数。" +
        "配送状态证据超过 30 秒未续接时中断计时。平台、类型、地区和时段内至少 20 个配对样本后，" +
        "评分可用聚合实耗进行保守校准。这是已接配送的样本，不代表所有报价，也不代表实际收入。",
        style = MaterialTheme.typography.bodySmall)
}
