package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.model.Valuation

@Composable
fun VehicleCostSummary(valuation: Valuation?) {
    val display = vehicleCostPresentation(valuation)
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("车辆成本估算 · USD", style = MaterialTheme.typography.titleSmall)
            Text("本单车辆成本 ${display.cost} · 扣成本贡献 ${display.contribution}",
                style = MaterialTheme.typography.bodyMedium)
            Text("扣车辆成本后时薪 ${display.hourly}/h", style = MaterialTheme.typography.bodyMedium)
            Text("按已保存的每英里成本和报价总路线计算。缺少可信里程或耗时时保留未知；" +
                "不含送达后等待、回程、税费和其他费用，不等于实际净利润。",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
