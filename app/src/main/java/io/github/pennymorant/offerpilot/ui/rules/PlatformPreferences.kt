@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.NumberField
import io.github.pennymorant.offerpilot.ui.components.ToggleRow
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun PlatformPreferences(state: RulesUiState, viewModel: RulesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(0, 2),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("订单类型开关", style = MaterialTheme.typography.titleMedium)
                    ToggleRow("允许自动抢雷达单（Trip Radar）", "拒单不扣接单率，建议设置更高加分门槛", state.allowTripRadar,
                        enabled = !state.saving, onChange = viewModel::allowTripRadar)
                    AnimatedVisibility(
                        visible = state.allowTripRadar,
                        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    ) {
                        NumberField("雷达单额外加分门槛（0–30）", state.radarExtraScore, "分",
                            { viewModel.edit(RuleField.RADAR_EXTRA_SCORE, it) }, enabled = !state.saving,
                            error = state.fieldErrors[RuleField.RADAR_EXTRA_SCORE])
                    }
                    ToggleRow("允许商超采购单（Shop & Pay）", "雨天与高小费大单常见来源", state.allowShopAndPay,
                        enabled = !state.saving, onChange = viewModel::allowShopAndPay)
                    AnimatedVisibility(
                        visible = state.allowShopAndPay,
                        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    ) {
                        NumberField("商超最大件数上限（留空不限）", state.maxShopItems, "件",
                            { viewModel.edit(RuleField.MAX_SHOP_ITEMS, it) }, enabled = !state.saving,
                            error = state.fieldErrors[RuleField.MAX_SHOP_ITEMS])
                    }
                    ToggleRow("允许包裹单（Package）", "关闭后跳过无小费长途包裹单", state.allowPackage,
                        enabled = !state.saving, onChange = viewModel::allowPackage)
                    ToggleRow("允许叠单", "按整组报价与路线评估", state.stacked,
                        enabled = !state.saving, onChange = viewModel::stacked)
                    ToggleRow("允许追加单", "增量收入及路线无法确认时跳过", state.addon,
                        enabled = !state.saving, onChange = viewModel::addon)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(1, 2),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("关键词与商圈过滤", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.excluded,
                        onValueChange = { viewModel.edit(RuleField.EXCLUDED, it) },
                        label = { Text("排除目的地关键词，逗号分隔") },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
                    )
                    OutlinedTextField(
                        value = state.excludedMerchants,
                        onValueChange = { viewModel.edit(RuleField.MERCHANTS, it) },
                        label = { Text("排除商家关键词，逗号分隔") },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
                    )
                    OutlinedTextField(
                        value = state.preferredZones,
                        onValueChange = { viewModel.edit(RuleField.PREFERRED_ZONES, it) },
                        label = { Text("优选商圈 / 高小费商家，逗号分隔") },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
                    )
                }
            }
        }
    }
}
