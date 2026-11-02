package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.model.Prop22Estimate

@Composable fun Prop22Summary(estimate: Prop22Estimate?) {
    estimate ?: return
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Prop 22 · 2026 单单情景估算", style = MaterialTheme.typography.titleSmall)
            Text("额外补差参考 ${prop22Money(estimate.extraCents)}", style = MaterialTheme.typography.titleMedium)
            prop22Issue(estimate)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (estimate.floorCents != null) {
                Text("保底参考 ${prop22Money(estimate.floorCents)}：时间 ${prop22Money(estimate.timeFloorCents)}" +
                    " + 里程 ${prop22Money(estimate.mileageFloorCents)}")
            }
            if (estimate.extraCents != null) {
                Text("假设平台收入 ${prop22Money(estimate.assumedPlatformPayCents)}，" +
                    "小费 ${prop22Money(estimate.assumedTipCents)}（报价的 ${estimate.assumedTipPercent}%）")
                Text("报价 + 情景补差 ${prop22Money(estimate.totalCents)}")
            }
            Text("采用配置最低时薪 ${money(estimate.minimumWageCents)}/h × 120%，" +
                "以报价总耗时/总里程代替接单至送达的实际有效时间/里程；不含接单前空等。",
                style = MaterialTheme.typography.bodySmall)
            Text("小费比例是手动假设，不是识别结果。真实补差按平台结算周期核算，其他收入可能抵消差额，" +
                "实际可能为 0；这里的补差不可逐单相加作为应收款。", style = MaterialTheme.typography.bodySmall)
        }
    }
}
