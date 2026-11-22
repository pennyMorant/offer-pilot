@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.NumberField
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

private val scorePresets = listOf("60", "70", "80", "85")

@Composable fun ValueScoreFields(state: RulesUiState, onField: (RuleField, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(0, 2),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("自动接单最低价值分", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        scorePresets.forEachIndexed { idx, preset ->
                            val shapes = when (idx) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                scorePresets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            ToggleButton(
                                checked = state.minimumScore.trim() == preset,
                                onCheckedChange = { onField(RuleField.MINIMUM_SCORE, preset) },
                                enabled = !state.saving,
                                shapes = shapes,
                                modifier = Modifier.weight(1f),
                            ) { Text("${preset}分", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        }
                    }
                    NumberField("自定义最低价值分", state.minimumScore, "分",
                        { onField(RuleField.MINIMUM_SCORE, it) }, enabled = !state.saving,
                        error = state.fieldErrors[RuleField.MINIMUM_SCORE])
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(1, 2),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("评分参考目标（达标计 70 分）", style = MaterialTheme.typography.titleMedium)
                    NumberField("目标成本后贡献时薪", state.targetContributionHour, "$/h",
                        { onField(RuleField.TARGET_CONTRIBUTION_HOUR, it) }, enabled = !state.saving,
                        error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION_HOUR])
                    NumberField("目标成本后每英里贡献", state.targetContributionMile, "$/mi",
                        { onField(RuleField.TARGET_CONTRIBUTION_MILE, it) }, enabled = !state.saving,
                        error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION_MILE])
                    NumberField("目标单笔成本后贡献", state.targetContribution, "$",
                        { onField(RuleField.TARGET_CONTRIBUTION, it) }, enabled = !state.saving,
                        error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION])
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                "达到各项目标时该项为 70 分，达到目标 1.5 倍为 100 分。默认权重：时薪 50% · 每英里贡献 35% · 单笔贡献 15%。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
