@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.ToolBadge
import com.overud.ui.components.groupedItemShape

@Composable
fun WorkSessionCard(state: DashboardUiState, onWork: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (state.working) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "workBg",
    )
    val fg = if (state.working) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = bg, contentColor = fg, shape = groupedItemShape(0, 2), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (state.working) "监听运行中" else "待机中", style = MaterialTheme.typography.titleLarge)
                        ToolBadge(
                            text = if (state.working) "ACTIVE" else "IDLE",
                            containerColor = if (state.working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (state.working) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
                }
            }
            Button(
                onClick = onWork,
                enabled = state.working || state.connection.authorized,
                shape = RoundedCornerShape(14.dp),
                colors = if (state.working) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(if (state.working) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (state.working) "结束工作" else "开始工作", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
