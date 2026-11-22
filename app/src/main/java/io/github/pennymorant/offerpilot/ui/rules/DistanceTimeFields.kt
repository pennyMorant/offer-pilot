package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.RuleCondition

@Composable
fun DistanceTimeFields(
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        RuleStepperCard(
            title = "最大总里程", value = state.distance, unit = "mi",
            step = 1.0, min = 1.0, max = 30.0, decimals = 1,
            presets = listOf("3 mi" to "3", "5 mi" to "5", "8 mi" to "8", "10 mi" to "10"),
            enabled = !state.saving, error = state.fieldErrors[RuleField.DISTANCE],
            onValue = { onField(RuleField.DISTANCE, it) },
            condition = RuleCondition.MAXIMUM_DISTANCE,
            active = RuleCondition.MAXIMUM_DISTANCE !in state.disabledConditions,
            onCondition = onCondition, index = 0, count = 3,
        )
        RuleStepperCard(
            title = "最长订单耗时", value = state.time, unit = "min",
            step = 5.0, min = 10.0, max = 120.0, decimals = 0,
            presets = listOf("20m" to "20", "30m" to "30", "45m" to "45", "60m" to "60"),
            enabled = !state.saving, error = state.fieldErrors[RuleField.TIME],
            onValue = { onField(RuleField.TIME, it) },
            condition = RuleCondition.MAXIMUM_DURATION,
            active = RuleCondition.MAXIMUM_DURATION !in state.disabledConditions,
            onCondition = onCondition, index = 1, count = 3,
        )
        RuleStepperCard(
            title = "最大取餐距离", value = state.pickup, unit = "mi",
            step = 0.5, min = 0.5, max = 15.0, decimals = 1,
            presets = listOf("1 mi" to "1", "2 mi" to "2", "3 mi" to "3", "5 mi" to "5"),
            enabled = !state.saving, error = state.fieldErrors[RuleField.PICKUP],
            onValue = { onField(RuleField.PICKUP, it) },
            condition = RuleCondition.MAXIMUM_PICKUP,
            active = RuleCondition.MAXIMUM_PICKUP !in state.disabledConditions,
            onCondition = onCondition, index = 2, count = 3,
        )
    }
}
