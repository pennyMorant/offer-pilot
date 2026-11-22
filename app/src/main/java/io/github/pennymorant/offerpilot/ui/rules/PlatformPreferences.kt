@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val shopPresets = listOf("5件" to "5", "10件" to "10", "15件" to "15", "不限" to "")

@Composable
fun PlatformPreferences(state: RulesUiState, viewModel: RulesViewModel) {
    val radarBonus = state.radarExtraScore.trim().toIntOrNull()?.coerceIn(0, 30) ?: 5
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrefTile("雷达单", "拒单不扣 AR", state.allowTripRadar, !state.saving, Modifier.weight(1f), viewModel::allowTripRadar)
                PrefTile("商超采购", "高小费大单来源", state.allowShopAndPay, !state.saving, Modifier.weight(1f), viewModel::allowShopAndPay)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrefTile("包裹单", "长途配送件", state.allowPackage, !state.saving, Modifier.weight(1f), viewModel::allowPackage)
                PrefTile("叠单", "按整组评估", state.stacked, !state.saving, Modifier.weight(1f), viewModel::stacked)
            }
            PrefTile("追加单", "仅增量路线明确时评估", state.addon, !state.saving, Modifier.fillMaxWidth(), viewModel::addon)
        }
        AnimatedVisibility(
            visible = state.allowTripRadar,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("雷达单额外加分门槛", style = MaterialTheme.typography.titleMedium)
                        Text("+$radarBonus 分", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = radarBonus.toFloat(),
                        onValueChange = { viewModel.edit(RuleField.RADAR_EXTRA_SCORE, it.roundToInt().toString()) },
                        valueRange = 0f..30f, enabled = !state.saving, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = state.allowShopAndPay,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("商超最大件数上限", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        shopPresets.forEachIndexed { idx, (label, raw) ->
                            val shapes = when (idx) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                shopPresets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            ToggleButton(
                                checked = state.maxShopItems.trim() == raw,
                                onCheckedChange = { viewModel.edit(RuleField.MAX_SHOP_ITEMS, raw) },
                                enabled = !state.saving, shapes = shapes, modifier = Modifier.weight(1f),
                            ) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        }
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("关键词与商圈过滤", style = MaterialTheme.typography.titleMedium)
                KeywordChipEditor("排除目的地", "例如 Airport", state.excluded, !state.saving) { viewModel.edit(RuleField.EXCLUDED, it) }
                KeywordChipEditor("排除商家", "例如 Walmart", state.excludedMerchants, !state.saving) { viewModel.edit(RuleField.MERCHANTS, it) }
                KeywordChipEditor("优选商圈 / 商家", "例如 Arcadia", state.preferredZones, !state.saving) { viewModel.edit(RuleField.PREFERRED_ZONES, it) }
            }
        }
    }
}

@Composable
private fun PrefTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onChange: (Boolean) -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "prefBg",
    )
    val fg = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = { onChange(!checked) }, enabled = enabled, color = bg, contentColor = fg,
        shape = RoundedCornerShape(if (checked) 24.dp else 18.dp), modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.78f), maxLines = 1)
        }
    }
}
