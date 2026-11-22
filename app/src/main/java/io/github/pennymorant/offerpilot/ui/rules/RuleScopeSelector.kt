@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.ToggleRow

@Composable
fun RuleScopeSelector(state: RulesUiState, onScope: (Platform?) -> Unit, onInherit: (Boolean) -> Unit) {
    val scopes = listOf<Platform?>(null) + Platform.entries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            scopes.forEachIndexed { index, platform ->
                val shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    scopes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                ToggleButton(
                    checked = state.scope == platform,
                    onCheckedChange = { onScope(platform) },
                    enabled = !state.saving,
                    shapes = shapes,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(platform?.label ?: "公共要求", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
        AnimatedVisibility(
            visible = state.scope != null,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    ToggleRow("跟随公共要求", "关闭后可为此平台单独设置条件", state.inheritShared,
                        enabled = !state.saving, onChange = onInherit)
                }
            }
        }
        Text("切换范围会保留草稿；保存时统一写入所有修改。", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
