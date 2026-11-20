package com.overud.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.overud.data.preferences.ArGuardState
import com.overud.model.DrivingScene
import com.overud.ui.components.ToolBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneAndArCard(
    activeScene: DrivingScene?,
    arGuard: ArGuardState,
    sessionSeen: Int,
    sessionAccepted: Int,
    onScene: (DrivingScene) -> Unit,
    onAdjustAr: (Int) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LA 策略档位", style = MaterialTheme.typography.titleMedium)
                ToolBadge(text = activeScene?.label ?: "自定义参数")
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DrivingScene.entries.forEachIndexed { index, scene ->
                    SegmentedButton(
                        selected = activeScene == scene,
                        onClick = { onScene(scene) },
                        shape = SegmentedButtonDefaults.itemShape(index, DrivingScene.entries.size),
                        icon = {},
                        label = {
                            val short = when (scene) {
                                DrivingScene.PEAK_CHERRY_PICK -> "高峰挑单"
                                DrivingScene.OFF_PEAK_GOLD -> "平峰金卡"
                                DrivingScene.LA_RAIN_PROP22 -> "雨天保底"
                            }
                            Text(short, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        },
                    )
                }
            }
            activeScene?.let {
                Text(it.summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Gold AR 护航", style = MaterialTheme.typography.labelLarge)
                        Text("${arGuard.uberArPercent}%", style = MaterialTheme.typography.titleMedium,
                            color = if (arGuard.uberArPercent < arGuard.goldFloorPercent)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { (arGuard.uberArPercent / 50f).coerceIn(0f, 1f) },
                        color = when {
                            arGuard.uberArPercent < arGuard.goldFloorPercent -> MaterialTheme.colorScheme.error
                            arGuard.uberArPercent < arGuard.goldFloorPercent + 5 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().padding(end = 10.dp).height(6.dp).clip(RoundedCornerShape(4.dp)),
                    )
                    Text(arGuard.hint, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sessionSeen > 0) {
                        val rate = (sessionAccepted * 100) / sessionSeen.coerceAtLeast(1)
                        Text("本班次专属单：接 $sessionAccepted / 见 $sessionSeen（$rate%）",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(onClick = { onAdjustAr(-1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "-1%", modifier = Modifier.size(16.dp))
                    }
                    FilledTonalIconButton(onClick = { onAdjustAr(1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "+1%", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
