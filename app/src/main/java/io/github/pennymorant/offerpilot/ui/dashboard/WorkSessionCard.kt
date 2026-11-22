@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun WorkSessionCard(state: DashboardUiState, onWork: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (state.working) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "workBg",
    )
    val fg = if (state.working) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = bg, contentColor = fg, shape = groupedItemShape(0, 2), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedContent(
                        targetState = state.working,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "workTitle",
                    ) { working ->
                        Text(
                            if (working) "监听运行中" else "待机中",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    ToolBadge(
                        text = if (state.working) "ACTIVE" else "IDLE",
                        containerColor = if (state.working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (state.working) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.82f))
            }
            Button(
                onClick = onWork,
                enabled = state.working || state.connection.authorized,
                shapes = ButtonDefaults.shapes(),
                colors = if (state.working) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                AnimatedContent(
                    targetState = state.working,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "workButton",
                ) { working ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (working) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (working) "结束工作" else "开始工作", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
