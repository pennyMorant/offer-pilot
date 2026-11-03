package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import com.overud.model.HourlyBasis
import androidx.compose.ui.unit.dp
import com.overud.model.RuleCondition
import com.overud.ui.components.NumberField

@Composable
fun EarningsFields(
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
    onHourlyBasis: (HourlyBasis) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RuleConditionField("最低报价", state.minimum, "$", RuleCondition.MINIMUM_AMOUNT,
            RuleField.MINIMUM, state, onField, onCondition)
        RuleConditionField("最低每英里报价", state.mile, "$/mi", RuleCondition.MINIMUM_PER_MILE,
            RuleField.MILE, state, onField, onCondition)
        Text("时薪门槛口径", style = MaterialTheme.typography.labelLarge)
        Column {
            HourlyBasis.entries.forEach { basis ->
                FilterChip(selected = state.hourlyBasis == basis,
                    onClick = { onHourlyBasis(basis) }, enabled = !state.saving,
                    label = { Text(if (basis == HourlyBasis.GROSS) "报价毛时薪" else "扣配置车辆成本后时薪") })
            }
        }
        NumberField("配置车辆成本", state.cost, "$/mi",
            { onField(RuleField.COST, it) }, enabled = !state.saving, error = state.fieldErrors[RuleField.COST])
        val hourlyLabel = if (state.hourlyBasis == HourlyBasis.GROSS) "最低预计毛时薪" else "最低扣车辆成本后时薪"
        RuleConditionField(hourlyLabel, state.hour, "$/h", RuleCondition.MINIMUM_HOURLY,
            RuleField.HOUR, state, onField, onCondition)
        RuleConditionField("最低单笔成本后贡献", state.minimumContribution, "$", RuleCondition.MINIMUM_CONTRIBUTION,
            RuleField.MINIMUM_CONTRIBUTION, state, onField, onCondition)
        Text("成本后时薪按可信总里程、配置的每英里成本及报价总耗时计算；" +
            "不含送达后等待、回程、停车费和税费，不等于实际净时薪。",
            style = MaterialTheme.typography.bodySmall)
    }
}
