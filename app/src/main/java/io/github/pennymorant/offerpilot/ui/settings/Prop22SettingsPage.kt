@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToggleRow
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape
import io.github.pennymorant.offerpilot.ui.rules.Prop22Input
import io.github.pennymorant.offerpilot.ui.rules.RuleStepperCard
import kotlin.math.roundToInt

private val wagePresets = listOf("州底薪" to "16.90", "17.87" to "17.87", "18.04" to "18.04", "19.65" to "19.65")
private val tipPresets = listOf("0%" to "0", "15%" to "15", "25%" to "25", "40%" to "40")

@Composable
fun Prop22SettingsPage(
    input: Prop22Input,
    saving: Boolean,
    error: String?,
    onChange: (Prop22Input) -> Unit,
    onSave: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (input.enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "propHeroBg",
    )
    val fg = if (input.enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val tipNum = input.tipPercent.trim().toIntOrNull()?.coerceIn(0, 100) ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                ToggleRow("启用加州 Prop 22 估算", "两个平台共用此参考参数，不计入自动接单门槛",
                    input.enabled, enabled = !saving) { onChange(input.copy(enabled = it)) }
            }
        }
        AnimatedVisibility(
            visible = input.enabled,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    RuleStepperCard(
                        title = "取餐地适用最低时薪", value = input.wage, prefix = "$", unit = "/h",
                        step = 0.10, min = 16.90, max = 35.0, decimals = 2,
                        presets = wagePresets, enabled = !saving, error = null,
                        onValue = { onChange(input.copy(wage = it)) }, index = 0, count = 3,
                    )
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(1, 3),
                        modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                            ToggleRow("使用机动车", "开启时按 2026 标准 $0.37/有效英里计算",
                                input.motorVehicle, enabled = !saving) { onChange(input.copy(motorVehicle = it)) }
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(2, 3),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("假设报价中的小费比例", style = MaterialTheme.typography.titleMedium)
                                Text("$tipNum%", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = tipNum.toFloat(),
                                onValueChange = { onChange(input.copy(tipPercent = it.roundToInt().toString())) },
                                valueRange = 0f..100f, enabled = !saving, modifier = Modifier.fillMaxWidth(),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                                tipPresets.forEachIndexed { idx, (label, raw) ->
                                    val shapes = when (idx) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        tipPresets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = tipNum.toString() == raw,
                                        onCheckedChange = { onChange(input.copy(tipPercent = raw)) },
                                        enabled = !saving, shapes = shapes, modifier = Modifier.weight(1f),
                                    ) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                                }
                            }
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("额外补差按单单情景估算；结算周期其他收入可能抵消差额，实际补差可能为 0。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp))
                }
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onSave,
            enabled = !saving,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (saving) "正在保存…" else "保存估算参数", style = MaterialTheme.typography.titleMedium)
        }
    }
}
