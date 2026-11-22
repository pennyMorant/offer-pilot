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
        RuleConditionField("最大总里程", state.distance, "mi", RuleCondition.MAXIMUM_DISTANCE,
            RuleField.DISTANCE, state, onField, onCondition, index = 0, count = 3)
        RuleConditionField("最长订单耗时", state.time, "min", RuleCondition.MAXIMUM_DURATION,
            RuleField.TIME, state, onField, onCondition, index = 1, count = 3)
        RuleConditionField("最大取餐距离", state.pickup, "mi", RuleCondition.MAXIMUM_PICKUP,
            RuleField.PICKUP, state, onField, onCondition, index = 2, count = 3)
    }
}
