@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.font.FontWeight
import com.overud.bridge.BridgeStatus
import com.overud.ui.components.*

@Composable fun DashboardScreen(state: DashboardUiState, bridge: BridgeStatus, onWork: () -> Unit,
    onAutomatic: (Boolean) -> Unit, onAuthorize: () -> Unit, onRules: () -> Unit, onReplay: () -> Unit, onResolve: () -> Unit) {
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("overud", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            StatusChip(if (bridge.serviceReady) "已连接" else "待连接", bridge.serviceReady)
        } }
        item { PageTitle("DRIVE WITH INTENTION", "每一程，都值得。", "看清订单价值，让接单更有把握。") }
        item { WorkSessionCard(state, onWork) }
        if (!bridge.serviceReady) item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("连接 Shevery", style = MaterialTheme.typography.titleMedium)
                    Text(bridge.message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onAuthorize) { Text("检查连接并授权") }
                }
            }
        }
        item { AutoAcceptCard(state, onAutomatic, onRules, onResolve) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("最新报价", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReplay) { Text("规则预览") }
        } }
        item {
            state.latest?.let { offer -> OfferCard(offer, state.decision) }
                ?: EmptyStateCard("等待第一张报价", "开始工作后，在 Uber 或 DoorDash 打开订单报价，分析结果会出现在这里。")
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("90 天报价", state.recordCount.toString(), Modifier.weight(1f))
            MetricCard("优质报价", state.goodCount.toString(), Modifier.weight(1f))
        } }
    }
}
