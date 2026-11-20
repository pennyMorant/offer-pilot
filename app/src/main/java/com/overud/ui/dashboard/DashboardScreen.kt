package com.overud.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.bridge.BridgeStatus
import com.overud.model.DrivingScene
import com.overud.ui.components.*

@Composable fun DashboardScreen(state: DashboardUiState, bridge: BridgeStatus, onWork: () -> Unit,
    onAutomatic: (Boolean) -> Unit, onAuthorize: () -> Unit, onRules: () -> Unit, onResolve: () -> Unit,
    onScene: (DrivingScene) -> Unit) {
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!bridge.serviceReady) item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(bridge.message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onAuthorize) { Text("连接与授权") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkSessionCard(state, onWork)
                AutoAcceptCard(state, onAutomatic, onRules, onResolve)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LA 场景策略一键切换", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrivingScene.entries.forEach { scene ->
                        AssistChip(
                            onClick = { onScene(scene) },
                            label = { Text(scene.label) },
                        )
                    }
                }
            }
        }
        item { Text("当前订单", style = MaterialTheme.typography.titleMedium) }
        item {
            state.latest?.let { OfferCard(it, state.decision, state.destinationAssessment) } ?: Column {
                Text(if (state.working) "等待新报价" else "开始工作后显示订单分析", style = MaterialTheme.typography.titleLarge)
                Text(if (state.working) "请在 Uber 或 DoorDash 打开报价页面。" else "连接 Shevery，然后开始工作。",
                    Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
