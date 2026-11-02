@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.font.FontWeight
import com.overud.work.WorkState
import com.overud.bridge.BridgeStatus
import com.overud.ui.components.*

@Composable fun DashboardScreen(state: WorkState, bridge: BridgeStatus, onWork: () -> Unit,
    onAutomatic: (Boolean) -> Unit, onAuthorize: () -> Unit, onRules: () -> Unit, onReplay: () -> Unit, onResolve: () -> Unit) {
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("overud", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            StatusChip(if (bridge.serviceReady) "已连接" else "待连接", bridge.serviceReady)
        } }
        item { PageTitle("DRIVE WITH INTENTION", "每一程，都值得。", "看清订单价值，让接单更有把握。") }
        item {
            val bg by animateColorAsState(if (state.working) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, label = "work")
            Card(colors = CardDefaults.cardColors(containerColor = bg), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (state.working) "工作进行中" else "准备好出发了吗？", style = MaterialTheme.typography.titleLarge)
                        if (state.working) LoadingIndicator(Modifier.size(34.dp))
                    }
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onWork, enabled = state.working || bridge.authorized,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (state.working) "结束工作" else "开始工作") }
                }
            }
        }
        if (!bridge.serviceReady) item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("连接 Shevery", style = MaterialTheme.typography.titleMedium)
                    Text(bridge.message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onAuthorize) { Text("检查连接并授权") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("自动接单", style = MaterialTheme.typography.titleLarge)
                            Text(if (state.automatic) "符合全部要求时直接接单" else "开启后按自定义要求执行", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = state.automatic, onCheckedChange = onAutomatic,
                            enabled = state.automatic || (state.working && bridge.serviceReady && !state.pending))
                    }
                    HorizontalDivider()
                    Text("≥ ${money(state.policy.minimumCents)}  ·  ≥ ${money(state.policy.minimumCentsPerMile)}/mi  ·  ≥ ${money(state.policy.minimumCentsPerHour)}/h",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onRules) { Text("调整接单要求 →") }
                    if (state.pending) FilledTonalButton(onClick = onResolve) { Text("核对接单结果 / 配送状态") }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("最新报价", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReplay) { Text("规则预览") }
        } }
        item {
            state.latest?.let { offer -> OfferCard(offer, state.decision) }
                ?: EmptyStateCard("等待第一张报价", "开始工作后，在 Uber 或 DoorDash 打开订单报价，分析结果会出现在这里。")
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("已记录报价", state.records.size.toString(), Modifier.weight(1f))
            MetricCard("符合当前记录规则", state.records.count { it.eligible }.toString(), Modifier.weight(1f))
        } }
    }
}
