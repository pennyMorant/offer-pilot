@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.RuleCondition
import io.github.pennymorant.offerpilot.ui.components.NumberField
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

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
    index: Int = 0,
    count: Int = 1,
) {
    val active = condition !in state.disabledConditions
    val bg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "condBg",
    )
    Surface(
        color = bg,
        shape = groupedItemShape(index, count),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!active) {
                        Text("未启用 · 不参与接单判断", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = active, onCheckedChange = { onCondition(condition, it) }, enabled = !state.saving)
            }
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                NumberField(title, value, unit, { onField(field, it) }, enabled = !state.saving, error = state.fieldErrors[field])
            }
        }
    }
}
