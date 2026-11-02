package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.overud.model.RuleCondition

@Composable
fun DistanceTimeFields(
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RuleConditionField("最大总里程", state.distance, "mi", RuleCondition.MAXIMUM_DISTANCE,
            RuleField.DISTANCE, state, onField, onCondition)
        RuleConditionField("最长订单耗时", state.time, "min", RuleCondition.MAXIMUM_DURATION,
            RuleField.TIME, state, onField, onCondition)
        RuleConditionField("最大取餐距离", state.pickup, "mi", RuleCondition.MAXIMUM_PICKUP,
            RuleField.PICKUP, state, onField, onCondition)
    }
}
