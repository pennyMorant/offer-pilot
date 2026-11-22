package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.RuleCondition
import io.github.pennymorant.offerpilot.ui.components.NumberField

@Composable
fun RuleConditionField(
    title: String,
    value: String,
    unit: String,
    condition: RuleCondition,
    field: RuleField,
    state: RulesUiState,
    onField: (RuleField, String) -> Unit,
    onCondition: (RuleCondition, Boolean) -> Unit,
) {
    val active = condition !in state.disabledConditions
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = active, onCheckedChange = { onCondition(condition, it) }, enabled = !state.saving)
            }
            if (active) {
                NumberField(title, value, unit, { onField(field, it) }, enabled = !state.saving, error = state.fieldErrors[field])
            } else {
                Text("此条件不参与接单判断", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
