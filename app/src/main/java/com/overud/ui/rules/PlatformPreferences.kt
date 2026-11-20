package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.ToggleRow

@Composable
fun PlatformPreferences(state: RulesUiState, viewModel: RulesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleRow("允许自动抢雷达单（Trip Radar）", "拒单不扣接单率，建议设置更高加分门槛", state.allowTripRadar,
            enabled = !state.saving, onChange = viewModel::allowTripRadar)
        if (state.allowTripRadar) {
            val radarErr = state.fieldErrors[RuleField.RADAR_EXTRA_SCORE]
            OutlinedTextField(
                value = state.radarExtraScore,
                onValueChange = { viewModel.edit(RuleField.RADAR_EXTRA_SCORE, it) },
                label = { Text("雷达单额外加分门槛（0–30 分）") },
                isError = radarErr != null,
                supportingText = radarErr?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
            )
        }
        ToggleRow("允许商超采购单（Shop & Pay）", "LA 雨天与富人区高小费大单来源", state.allowShopAndPay,
            enabled = !state.saving, onChange = viewModel::allowShopAndPay)
        if (state.allowShopAndPay) {
            val itemErr = state.fieldErrors[RuleField.MAX_SHOP_ITEMS]
            OutlinedTextField(
                value = state.maxShopItems,
                onValueChange = { viewModel.edit(RuleField.MAX_SHOP_ITEMS, it) },
                label = { Text("商超最大件数上限（留空不限）") },
                isError = itemErr != null,
                supportingText = itemErr?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
            )
        }
        ToggleRow("允许包裹单（Package）", "关闭后跳过无小费长途包裹单", state.allowPackage,
            enabled = !state.saving, onChange = viewModel::allowPackage)
        ToggleRow("允许叠单", "按整组报价与路线评估", state.stacked,
            enabled = !state.saving, onChange = viewModel::stacked)
        ToggleRow("允许追加单", "增量收入及路线无法确认时跳过", state.addon,
            enabled = !state.saving, onChange = viewModel::addon)
        OutlinedTextField(
            value = state.excluded,
            onValueChange = { viewModel.edit(RuleField.EXCLUDED, it) },
            label = { Text("排除目的地关键词，用逗号分隔") },
            modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
        )
        OutlinedTextField(
            value = state.excludedMerchants,
            onValueChange = { viewModel.edit(RuleField.MERCHANTS, it) },
            label = { Text("排除商家关键词，用逗号分隔") },
            modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
        )
    }
}
