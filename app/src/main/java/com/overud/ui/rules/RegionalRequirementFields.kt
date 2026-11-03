package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.overud.ui.components.NumberField
import com.overud.ui.components.ToggleRow

@Composable
fun RegionalRequirementFields(
    state: RulesUiState,
    onEnabled: (Boolean) -> Unit,
    onField: (RuleField, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleRow("要求目的地历史报价质量达标", "可选；开启后证据不足的报价不会自动接单",
            state.regionalEnabled, enabled = !state.saving, onChange = onEnabled)
        if (state.regionalEnabled) {
            NumberField("最低历史优质报价比例（1–100）", state.regionalGoodPercent, "%",
                { onField(RuleField.REGION_PERCENT, it) }, enabled = !state.saving, error = state.fieldErrors[RuleField.REGION_PERCENT])
            NumberField("最低有效分类样本（10–100000）", state.regionalSamples, "张",
                { onField(RuleField.REGION_SAMPLES, it) }, enabled = !state.saving, error = state.fieldErrors[RuleField.REGION_SAMPLES])
            Text("仅使用同平台、目的地所在网格的送达时段历史。需要地址或坐标精度和可信总耗时；" +
                "解析中、样本不足或只剩地区整体基线时均不通过。历史优质比例不是下一单概率。",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
