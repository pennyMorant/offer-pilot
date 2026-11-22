@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.DrivingScene
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.OfferCard

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onResolve: () -> Unit,
    onScene: (DrivingScene) -> Unit,
    onAdjustAr: (Int) -> Unit,
    onOpenPlatform: (Platform) -> Unit,
) {
    val alert = state.backgroundAlert?.takeIf { System.currentTimeMillis() - it.receivedAt in 0..35_000L }
    LazyColumn(
        Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AnimatedVisibility(
                visible = alert != null,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                alert?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                "${it.platform.label} 后台来单${it.amountPreview?.let { p -> " $p" }.orEmpty()}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            FilledTonalButton(
                                onClick = { onOpenPlatform(it.platform) },
                                shapes = ButtonDefaults.shapes(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text("查看", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        item { WorkSessionCard(state) }
        item {
            AnimatedVisibility(
                visible = state.pending,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("订单状态待核对", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = onResolve, shapes = ButtonDefaults.shapes()) { Text("核对") }
                    }
                }
            }
        }
        item {
            state.latest?.let {
                OfferCard(it, state.decision, state.destinationAssessment, state.arGuard.uberArPercent,
                    state.policy.effective(it.platform).preferredZones)
            } ?: Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Rounded.Radar, contentDescription = null, modifier = Modifier.padding(12.dp).size(24.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (state.working) "等待报价弹窗…" else "实时报价仪表盘", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.working) "打开 Uber Eats 或 DoorDash 报价页面，将自动计算真实收益。"
                            else "点击右下角按钮开始工作。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { SceneAndArCard(state.activeScene, state.arGuard, state.sessionExclusiveSeen, state.sessionExclusiveAccepted, onScene, onAdjustAr) }
    }
}
