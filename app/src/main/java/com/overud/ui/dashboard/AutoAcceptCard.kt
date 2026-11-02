package com.overud.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.money

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
                        text = if (state.automatic) "符合全部要求时直接接单" else "开启后按自定义要求执行",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = state.automatic,
                    onCheckedChange = onAutomatic,
                    enabled = state.automatic || (state.working && state.connection.serviceReady && !state.pending),
                )
            }
            HorizontalDivider()
            Text(
                text = "≥ ${money(state.policy.minimumCents)}  ·  " +
                    "≥ ${money(state.policy.minimumCentsPerMile)}/mi  ·  " +
                    "≥ ${money(state.policy.minimumCentsPerHour)}/h",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onRules) { Text("调整接单要求 →") }
            if (state.pending) {
                FilledTonalButton(onClick = onResolve) { Text("核对接单结果 / 配送状态") }
            }
        }
    }
}
