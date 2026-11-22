@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.DeliveryPhase
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun AutoAcceptCard(
    state: DashboardUiState,
    onAutomatic: (Boolean) -> Unit,
    onRules: () -> Unit,
    onResolve: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(1, 2),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = if (state.automatic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (state.automatic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) { Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("自动接单", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = when {
                            state.policySaving -> "保存规则中，已暂停"
                            state.deliveryPhase == DeliveryPhase.ACTIVE && state.autoAcceptEnabled -> "配送中暂停，送达后自动恢复"
                            state.pending -> "订单占用中，等待核对状态"
                            state.automatic -> "运行中 · 全部启用条件同时满足时执行"
                            state.autoAcceptEnabled -> "待命 · 切回平台自动恢复"
                            else -> "关闭 · 仅展示分析与语音"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.autoAcceptEnabled,
                    onCheckedChange = onAutomatic,
                    enabled = state.autoAcceptEnabled || (state.working && state.connection.serviceReady &&
                        !state.pending && !state.policySaving),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onRules,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("规则参数", style = MaterialTheme.typography.labelLarge)
                }
                AnimatedVisibility(
                    visible = state.pending,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                        expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                        shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()),
                ) {
                    Button(
                        onClick = onResolve,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("核对配送状态", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
