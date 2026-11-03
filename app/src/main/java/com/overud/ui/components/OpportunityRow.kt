package com.overud.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun OpportunityRow(state: OpportunityCardState) {
    var detail by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clickable { detail = true }.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium)
                Text(state.subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(state.score?.let { "$it 分" } ?: "—", style = MaterialTheme.typography.titleLarge)
        }
        Text(listOfNotNull(
            state.goodRate?.let { "优质 $it%" },
            state.offersPerHour?.let { String.format(Locale.getDefault(), "每小时 %.1f 张", it) },
            state.confidence,
        ).joinToString(" · "))
        Text(state.advice, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
    if (detail) AlertDialog(
        onDismissRequest = { detail = false },
        title = { Text("区域机会详情") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium)
                Text(state.subtitle)
                Text("区域机会指数 ${state.score?.let { "$it / 100" } ?: "暂不可用"} · ${state.confidence}")
                Text(state.goodRate?.let { "历史优质比例 $it%" } ?: "历史优质比例样本不足")
                Text(state.offersPerHour?.let { String.format(Locale.getDefault(), "每观察小时 %.1f 张报价", it) }
                    ?: "观察时间不足 1 小时")
                Text("毛报价均值 ${state.averageMile}/mi · ${state.averageHour}/h")
                Text(state.samples, style = MaterialTheme.typography.bodySmall)
                Text(state.advice)
                Text("指数描述历史观察，不是下一单概率；前往成本和实时供需尚未计入。",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { detail = false }) { Text("关闭") } },
    )
}
