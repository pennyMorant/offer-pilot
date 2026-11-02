package com.overud.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.model.DeliveryPhase

@Composable
fun AutoAcceptCard(
    state: DashboardUiState,
    onAutomatic: (Boolean) -> Unit,
    onRules: () -> Unit,
    onResolve: () -> Unit,
) {
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("自动接单", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = when {
                            state.policySaving -> "正在保存规则，自动接单暂停"
                            state.deliveryPhase == DeliveryPhase.ACTIVE && state.autoAcceptEnabled -> "配送中暂停，确认送达后继续"
                            state.pending -> "订单占用中，等待确认配送状态"
                            state.automatic -> "符合全部开启条件时直接接单"
                            else -> "开启后按已保存要求执行"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = state.autoAcceptEnabled,
                    onCheckedChange = onAutomatic,
                    enabled = state.autoAcceptEnabled || (state.working && state.connection.serviceReady &&
                        !state.pending && !state.policySaving),
                )
            }
            HorizontalDivider()
            state.policySummaries.forEach { summary ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(summary.label, style = MaterialTheme.typography.titleSmall)
                    Text(summary.details, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            TextButton(onClick = onRules) { Text("调整接单要求 →") }
            if (state.pending) {
                FilledTonalButton(onClick = onResolve) { Text("核对接单结果 / 配送状态") }
            }
        }
    }
}
