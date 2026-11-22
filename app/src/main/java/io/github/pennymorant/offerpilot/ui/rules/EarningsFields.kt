@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.HourlyBasis
import io.github.pennymorant.offerpilot.model.RuleCondition
import io.github.pennymorant.offerpilot.ui.components.NumberField
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun EarningsFields(
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
    onHourlyBasis: (HourlyBasis) -> Unit,
) {
    val hourlyLabel = if (state.hourlyBasis == HourlyBasis.GROSS) "最低预计毛时薪" else "最低扣车辆成本后时薪"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RuleConditionField("最低报价", state.minimum, "$", RuleCondition.MINIMUM_AMOUNT,
                RuleField.MINIMUM, state, onField, onCondition, index = 0, count = 2)
            RuleConditionField("最低每英里报价", state.mile, "$/mi", RuleCondition.MINIMUM_PER_MILE,
                RuleField.MILE, state, onField, onCondition, index = 1, count = 2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(0, 3),
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
                                enabled = !state.saving,
                                shapes = shapes,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(if (basis == HourlyBasis.GROSS) "报价毛时薪" else "扣成本后时薪",
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                    NumberField("配置车辆成本", state.cost, "$/mi",
                        { onField(RuleField.COST, it) }, enabled = !state.saving, error = state.fieldErrors[RuleField.COST])
                }
            }
            RuleConditionField(hourlyLabel, state.hour, "$/h", RuleCondition.MINIMUM_HOURLY,
                RuleField.HOUR, state, onField, onCondition, index = 1, count = 3)
            RuleConditionField("最低单笔成本后贡献", state.minimumContribution, "$", RuleCondition.MINIMUM_CONTRIBUTION,
                RuleField.MINIMUM_CONTRIBUTION, state, onField, onCondition, index = 2, count = 3)
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                "成本后时薪按可信总里程、配置的每英里成本及报价总耗时计算；不含送达后等待、回程、停车费和税费，不等于实际净时薪。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
