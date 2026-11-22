@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.RuleCondition
import io.github.pennymorant.offerpilot.ui.components.NumberField
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape
import java.util.Locale

@Composable
fun RuleStepperCard(
    title: String,
    value: String,
    prefix: String = "",
    unit: String,
    step: Double,
    min: Double,
    max: Double,
    decimals: Int = 2,
    presets: List<Pair<String, String>>,
    enabled: Boolean,
    error: String?,
    onValue: (String) -> Unit,
    condition: RuleCondition? = null,
    active: Boolean = true,
    onCondition: ((RuleCondition, Boolean) -> Unit)? = null,
    index: Int = 0,
    count: Int = 1,
) {
    var editing by remember(title) { mutableStateOf(false) }
    val numeric = value.trim().toDoubleOrNull()
    fun formatNum(v: Double): String = if (decimals == 0) v.toInt().toString()
        else String.format(Locale.US, "%.${decimals}f", v).trimEnd('0').trimEnd('.')

    val bg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "stepperBg",
    )
    Surface(color = bg, shape = groupedItemShape(index, count), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!active) {
                        Text("未启用 · 不参与接单判断", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (condition != null && onCondition != null) {
                    Switch(checked = active, onCheckedChange = { onCondition(condition, it) }, enabled = enabled)
                }
            }
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        FilledTonalIconButton(
                            onClick = { onValue(formatNum(((numeric ?: min) - step).coerceIn(min, max))) },
                            enabled = enabled,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(44.dp),
                        ) { Icon(Icons.Rounded.Remove, contentDescription = "减少", modifier = Modifier.size(20.dp)) }
                        Row(
                            Modifier.clip(MaterialTheme.shapes.medium).clickable(enabled = enabled) { editing = !editing }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "$prefix${value.ifBlank { "—" }}${if (prefix.isEmpty()) " $unit" else if (unit.startsWith("/")) unit else ""}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(Icons.Rounded.Edit, contentDescription = "精确输入",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        FilledTonalIconButton(
                            onClick = { onValue(formatNum(((numeric ?: min) + step).coerceIn(min, max))) },
                            enabled = enabled,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(44.dp),
                        ) { Icon(Icons.Rounded.Add, contentDescription = "增加", modifier = Modifier.size(20.dp)) }
                    }
                    if (presets.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            presets.forEachIndexed { idx, (label, raw) ->
                                val shapes = when (idx) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    presets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                                val match = numeric != null && raw.toDoubleOrNull()?.let { kotlin.math.abs(it - numeric) < 1e-4 } == true
                                ToggleButton(
                                    checked = match,
                                    onCheckedChange = { onValue(raw) },
                                    enabled = enabled,
                                    shapes = shapes,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                ) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                            }
                        }
                    }
                    AnimatedVisibility(visible = editing || error != null) {
                        NumberField(title, value, unit, onValue, enabled = enabled, error = error)
                    }
                }
            }
        }
    }
}
