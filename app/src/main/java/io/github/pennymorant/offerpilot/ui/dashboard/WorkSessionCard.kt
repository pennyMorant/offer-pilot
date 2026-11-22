@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
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
fun WorkSessionCard(state: DashboardUiState) {
    val bg by animateColorAsState(
        targetValue = if (state.working) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "workBg",
    )
    val fg = if (state.working) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedContent(
                    targetState = state.working,
                    transitionSpec = { fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec) },
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
    }
}
