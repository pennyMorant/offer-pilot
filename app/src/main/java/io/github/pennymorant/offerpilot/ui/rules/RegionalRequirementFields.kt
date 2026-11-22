@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToggleRow
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape
import kotlin.math.roundToInt

private val samplePresets = listOf("10张" to "10", "30张" to "30", "50张" to "50", "100张" to "100")

@Composable
fun RegionalRequirementFields(
    state: RulesUiState,
    onEnabled: (Boolean) -> Unit,
    onField: (RuleField, String) -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (state.regionalEnabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "regionHeroBg",
    )
    val fg = if (state.regionalEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val pct = state.regionalGoodPercent.trim().toIntOrNull()?.coerceIn(1, 100) ?: 60

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                ToggleRow("要求目的地历史报价质量达标", "开启后证据不足或弱区报价不自动接单",
                    state.regionalEnabled, enabled = !state.saving, onChange = onEnabled)
            }
        }
        AnimatedVisibility(
            visible = state.regionalEnabled,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(0, 2),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("最低历史优质报价占比", style = MaterialTheme.typography.titleMedium)
                                Text("$pct%", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = pct.toFloat(),
                                onValueChange = { onField(RuleField.REGION_PERCENT, it.roundToInt().toString()) },
                                valueRange = 20f..90f,
                                enabled = !state.saving,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(1, 2),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("最低有效分类样本数", style = MaterialTheme.typography.titleMedium)
                                Text("${state.regionalSamples.ifBlank { "—" }} 张",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                                samplePresets.forEachIndexed { idx, (label, raw) ->
                                    val shapes = when (idx) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        samplePresets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = state.regionalSamples.trim() == raw,
                                        onCheckedChange = { onField(RuleField.REGION_SAMPLES, raw) },
                                        enabled = !state.saving, shapes = shapes, modifier = Modifier.weight(1f),
                                    ) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                                }
                            }
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "仅使用同平台、目的地网格的送达时段历史。解析中或样本不足时自动跳过；历史优质比例不是下一单概率。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
}
