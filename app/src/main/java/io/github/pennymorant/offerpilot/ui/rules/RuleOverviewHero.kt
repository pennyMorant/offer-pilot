@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.RuleCondition
import io.github.pennymorant.offerpilot.ui.components.ToolBadge

@Composable
fun RuleOverviewHero(
    state: RulesUiState,
    dirty: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val activeCount = RuleCondition.entries.count { it !in state.disabledConditions }
    val scopeTitle = when {
        state.scope == null -> "公共接单策略"
        state.inheritShared -> "${state.scope.label} · 跟随公共"
        else -> "${state.scope.label} · 独立策略"
    }
    val bg by animateColorAsState(
        targetValue = if (dirty) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "heroBg",
    )
    val fg = if (dirty) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(scopeTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                ToolBadge(
                    text = if (dirty) "草稿未保存" else "已生效",
                    containerColor = if (dirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (dirty) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolBadge(text = "启用 $activeCount/${RuleCondition.entries.size} 项硬门槛")
                ToolBadge(text = "${state.platforms.size} 个平台生效")
                if (state.regionalEnabled) ToolBadge(text = "区域门槛 ${state.regionalGoodPercent}%")
            }
            AnimatedVisibility(
                visible = dirty,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDiscard,
                        enabled = !state.saving,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("放弃修改")
                    }
                    Button(
                        onClick = onSave,
                        enabled = !state.saving,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1.3f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.saving) "保存中…" else "保存策略")
                    }
                }
            }
        }
    }
}
