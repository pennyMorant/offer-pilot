@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.data.preferences.ArGuardState
import io.github.pennymorant.offerpilot.model.DrivingScene
import io.github.pennymorant.offerpilot.ui.components.ToolBadge

@Composable
fun SceneAndArCard(
    activeScene: DrivingScene?,
    arGuard: ArGuardState,
    sessionSeen: Int,
    sessionAccepted: Int,
    onScene: (DrivingScene) -> Unit,
    onAdjustAr: (Int) -> Unit,
) {
    val animatedAr by animateFloatAsState(
        targetValue = (arGuard.uberArPercent / 50f).coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "arProgress",
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LA 策略档位", style = MaterialTheme.typography.titleMedium)
                ToolBadge(text = activeScene?.label ?: "自定义参数")
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                DrivingScene.entries.forEachIndexed { index, scene ->
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        DrivingScene.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    val short = when (scene) {
                        DrivingScene.PEAK_CHERRY_PICK -> "高峰挑单"
                        DrivingScene.OFF_PEAK_GOLD -> "平峰金卡"
                        DrivingScene.LA_RAIN_PROP22 -> "雨天保底"
                    }
                    ToggleButton(
                        checked = activeScene == scene,
                        onCheckedChange = { onScene(scene) },
                        shapes = shapes,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(short, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
            activeScene?.let {
                Text(it.summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Gold AR 护航", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${arGuard.uberArPercent}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = if (arGuard.uberArPercent < arGuard.goldFloorPercent)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                    LinearWavyProgressIndicator(
                        progress = { animatedAr },
                        color = when {
                            arGuard.uberArPercent < arGuard.goldFloorPercent -> MaterialTheme.colorScheme.error
                            arGuard.uberArPercent < arGuard.goldFloorPercent + 5 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(arGuard.hint, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sessionSeen > 0) {
                        val rate = (sessionAccepted * 100) / sessionSeen.coerceAtLeast(1)
                        Text("本班次专属单：接 $sessionAccepted / 见 $sessionSeen（$rate%）",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = { onAdjustAr(-1) },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "-1%", modifier = Modifier.size(18.dp))
                    }
                    FilledTonalIconButton(
                        onClick = { onAdjustAr(1) },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "+1%", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
