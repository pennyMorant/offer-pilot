package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.NumberField
import com.overud.ui.components.ToggleRow

@Composable
fun PlatformPreferences(state: RulesUiState, viewModel: RulesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NumberField("车辆成本（用于贡献估计）", state.cost, "$/mi",
            { viewModel.edit(RuleField.COST, it) }, enabled = !state.saving)
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
