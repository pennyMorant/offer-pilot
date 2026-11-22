package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.bridge.BridgeStatus
import io.github.pennymorant.offerpilot.model.DrivingScene
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.OfferCard

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    bridge: BridgeStatus,
    onWork: () -> Unit,
    onAutomatic: (Boolean) -> Unit,
    onAuthorize: () -> Unit,
    onRules: () -> Unit,
    onResolve: () -> Unit,
    onScene: (DrivingScene) -> Unit,
    onAdjustAr: (Int) -> Unit,
    onOpenPlatform: (Platform) -> Unit,
) {
    LazyColumn(
        Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = if (bridge.serviceReady) MaterialTheme.colorScheme.surfaceContainerLow
                    else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (bridge.serviceReady) Icons.Rounded.CheckCircle else Icons.Rounded.LinkOff,
                        contentDescription = null,
                        tint = if (bridge.serviceReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(bridge.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    if (!bridge.serviceReady) {
                        FilledTonalButton(onClick = onAuthorize, shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("连接", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        state.backgroundAlert?.takeIf { System.currentTimeMillis() - it.receivedAt in 0..35_000L }?.let { alert ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("${alert.platform.label} 后台来单${alert.amountPreview?.let { " $it" }.orEmpty()}",
                            style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { onOpenPlatform(alert.platform) }, shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("查看", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                WorkSessionCard(state, onWork)
                AutoAcceptCard(state, onAutomatic, onRules, onResolve)
            }
        }
        item { SceneAndArCard(state.activeScene, state.arGuard, state.sessionExclusiveSeen, state.sessionExclusiveAccepted, onScene, onAdjustAr) }
        item {
            state.latest?.let {
                OfferCard(it, state.decision, state.destinationAssessment, state.arGuard.uberArPercent,
                    state.policy.effective(it.platform).preferredZones)
            } ?: Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (state.working) "等待报价弹窗…" else "实时报价仪表盘", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.working) "打开 Uber Eats 或 DoorDash 报价页面，将自动计算真实收益与假好单警示。"
                        else "连接 Shevery 无线调试后点击“开始工作”。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
