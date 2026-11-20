package com.overud.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.data.preferences.ArGuardState
import com.overud.model.DrivingScene

@Composable
fun SceneAndArCard(
    activeScene: DrivingScene?,
    arGuard: ArGuardState,
    onScene: (DrivingScene) -> Unit,
    onAdjustAr: (Int) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LA 场景策略一键切换", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrivingScene.entries.forEach { scene ->
                    FilterChip(
                        selected = activeScene == scene,
                        onClick = { onScene(scene) },
                        label = { Text(scene.label) },
                    )
                }
            }
            activeScene?.let {
                Text(it.summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Uber Gold AR 护航 · ${arGuard.uberArPercent}%", style = MaterialTheme.typography.labelLarge)
                    Text(arGuard.hint, style = MaterialTheme.typography.bodySmall,
                        color = if (arGuard.uberArPercent < arGuard.goldFloorPercent)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { onAdjustAr(-1) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("-1%") }
                    OutlinedButton(onClick = { onAdjustAr(1) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+1%") }
                }
            }
        }
    }
}
