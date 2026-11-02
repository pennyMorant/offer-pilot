package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.overud.model.RuleCondition

@Composable
fun EarningsFields(
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RuleConditionField("最低报价", state.minimum, "$", RuleCondition.MINIMUM_AMOUNT,
            RuleField.MINIMUM, state, onField, onCondition)
        RuleConditionField("最低每英里报价", state.mile, "$/mi", RuleCondition.MINIMUM_PER_MILE,
            RuleField.MILE, state, onField, onCondition)
        RuleConditionField("最低预计毛时薪", state.hour, "$/h", RuleCondition.MINIMUM_HOURLY,
            RuleField.HOUR, state, onField, onCondition)
    }
}
