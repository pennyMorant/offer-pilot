@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import kotlin.math.roundToInt

private val scorePresets = listOf("60", "70", "80", "85")

@Composable
fun ValueScoreFields(state: RulesUiState, onField: (RuleField, String) -> Unit) {
    val score = state.minimumScore.trim().toIntOrNull()?.coerceIn(1, 100) ?: 70
    val (gradeLabel, gradeBg, gradeFg) = when {
        score >= 85 -> Triple("严选高分单", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        score >= 70 -> Triple("均衡优质", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> Triple("宽松多接", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("自动接单最低价值分", style = MaterialTheme.typography.titleMedium)
                        Text("${score}分", style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold)
                    }
                    ToolBadge(gradeLabel, gradeBg, gradeFg)
                }
                Slider(
                    value = score.toFloat(),
                    onValueChange = { onField(RuleField.MINIMUM_SCORE, it.roundToInt().toString()) },
                    valueRange = 1f..100f,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    scorePresets.forEachIndexed { idx, preset ->
                        val shapes = when (idx) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            scorePresets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        ToggleButton(
                            checked = score.toString() == preset,
                            onCheckedChange = { onField(RuleField.MINIMUM_SCORE, preset) },
                            enabled = !state.saving, shapes = shapes, modifier = Modifier.weight(1f),
                        ) { Text("${preset}分", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                    }
                }
                Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.weight(0.50f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    Box(Modifier.weight(0.35f).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                    Box(Modifier.weight(0.15f).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                }
                Text("权重：时薪 50% · 单价 35% · 单笔 15%（达目标 70 分，1.5 倍 100 分）",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RuleStepperCard(
                title = "目标成本后贡献时薪", value = state.targetContributionHour, prefix = "$", unit = "/h",
                step = 2.0, min = 10.0, max = 80.0, decimals = 0,
                presets = listOf("$18" to "18", "$20" to "20", "$25" to "25", "$30" to "30"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION_HOUR],
                onValue = { onField(RuleField.TARGET_CONTRIBUTION_HOUR, it) }, index = 0, count = 3,
            )
            RuleStepperCard(
                title = "目标成本后每英里贡献", value = state.targetContributionMile, prefix = "$", unit = "/mi",
                step = 0.15, min = 0.5, max = 8.0, decimals = 2,
                presets = listOf("$1.5" to "1.5", "$1.65" to "1.65", "$2.0" to "2", "$2.5" to "2.5"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION_MILE],
                onValue = { onField(RuleField.TARGET_CONTRIBUTION_MILE, it) }, index = 1, count = 3,
            )
            RuleStepperCard(
                title = "目标单笔成本后贡献", value = state.targetContribution, prefix = "$", unit = "$",
                step = 1.0, min = 2.0, max = 40.0, decimals = 2,
                presets = listOf("$6" to "6", "$8" to "8", "$10" to "10", "$12" to "12"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION],
                onValue = { onField(RuleField.TARGET_CONTRIBUTION, it) }, index = 2, count = 3,
            )
        }
    }
}
