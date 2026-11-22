@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.HourlyBasis
import io.github.pennymorant.offerpilot.model.RuleCondition
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun EarningsFields(
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
    onHourlyBasis: (HourlyBasis) -> Unit,
) {
    val hourlyLabel = if (state.hourlyBasis == HourlyBasis.GROSS) "最低预计毛时薪" else "最低扣成本后时薪"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RuleStepperCard(
                title = "最低报价", value = state.minimum, prefix = "$", unit = "$",
                step = 1.0, min = 1.0, max = 50.0, decimals = 2,
                presets = listOf("$6" to "6", "$8" to "8", "$10" to "10", "$12" to "12"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.MINIMUM],
                onValue = { onField(RuleField.MINIMUM, it) },
                condition = RuleCondition.MINIMUM_AMOUNT,
                active = RuleCondition.MINIMUM_AMOUNT !in state.disabledConditions,
                onCondition = onCondition, index = 0, count = 2,
            )
            RuleStepperCard(
                title = "最低每英里报价", value = state.mile, prefix = "$", unit = "/mi",
                step = 0.25, min = 0.5, max = 10.0, decimals = 2,
                presets = listOf("$1.5" to "1.5", "$2.0" to "2", "$2.5" to "2.5", "$3.0" to "3"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.MILE],
                onValue = { onField(RuleField.MILE, it) },
                condition = RuleCondition.MINIMUM_PER_MILE,
                active = RuleCondition.MINIMUM_PER_MILE !in state.disabledConditions,
                onCondition = onCondition, index = 1, count = 2,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(0, 4),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("时薪门槛口径", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        HourlyBasis.entries.forEachIndexed { idx, basis ->
                            val shapes = if (idx == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                                else ButtonGroupDefaults.connectedTrailingButtonShapes()
                            ToggleButton(
                                checked = state.hourlyBasis == basis,
                                onCheckedChange = { onHourlyBasis(basis) },
                                enabled = !state.saving, shapes = shapes, modifier = Modifier.weight(1f),
                            ) { Text(if (basis == HourlyBasis.GROSS) "报价毛时薪" else "扣成本后时薪", maxLines = 1) }
                        }
                    }
                }
            }
            RuleStepperCard(
                title = "配置车辆成本", value = state.cost, prefix = "$", unit = "/mi",
                step = 0.05, min = 0.0, max = 3.0, decimals = 2,
                presets = listOf("$0.25" to "0.25", "$0.35" to "0.35", "$0.50" to "0.5", "$0.67" to "0.67"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.COST],
                onValue = { onField(RuleField.COST, it) }, index = 1, count = 4,
            )
            RuleStepperCard(
                title = hourlyLabel, value = state.hour, prefix = "$", unit = "/h",
                step = 1.0, min = 10.0, max = 80.0, decimals = 0,
                presets = listOf("$20" to "20", "$25" to "25", "$30" to "30", "$35" to "35"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.HOUR],
                onValue = { onField(RuleField.HOUR, it) },
                condition = RuleCondition.MINIMUM_HOURLY,
                active = RuleCondition.MINIMUM_HOURLY !in state.disabledConditions,
                onCondition = onCondition, index = 2, count = 4,
            )
            RuleStepperCard(
                title = "最低单笔成本后贡献", value = state.minimumContribution, prefix = "$", unit = "$",
                step = 1.0, min = 0.0, max = 40.0, decimals = 2,
                presets = listOf("$4" to "4", "$5" to "5", "$6" to "6", "$8" to "8"),
                enabled = !state.saving, error = state.fieldErrors[RuleField.MINIMUM_CONTRIBUTION],
                onValue = { onField(RuleField.MINIMUM_CONTRIBUTION, it) },
                condition = RuleCondition.MINIMUM_CONTRIBUTION,
                active = RuleCondition.MINIMUM_CONTRIBUTION !in state.disabledConditions,
                onCondition = onCondition, index = 3, count = 4,
            )
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                "成本后时薪按可信总里程、每英里成本及报价总耗时计算；不含送达后等待、回程、停车费和税费。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
