package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.overud.ui.components.NumberField

@Composable fun ValueScoreFields(state: RulesUiState, onField: (RuleField, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField("目标成本后贡献时薪", state.targetContributionHour, "$/h",
            { onField(RuleField.TARGET_CONTRIBUTION_HOUR, it) }, enabled = !state.saving,
            error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION_HOUR])
        NumberField("目标成本后每英里贡献", state.targetContributionMile, "$/mi",
            { onField(RuleField.TARGET_CONTRIBUTION_MILE, it) }, enabled = !state.saving,
            error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION_MILE])
        NumberField("目标单笔成本后贡献", state.targetContribution, "$",
            { onField(RuleField.TARGET_CONTRIBUTION, it) }, enabled = !state.saving,
            error = state.fieldErrors[RuleField.TARGET_CONTRIBUTION])
        NumberField("自动接单最低价值分", state.minimumScore, "分",
            { onField(RuleField.MINIMUM_SCORE, it) }, enabled = !state.saving,
            error = state.fieldErrors[RuleField.MINIMUM_SCORE])
        Text("达到各项目标时，该项为 70 分；目标的 1.5 倍为 100 分。" +
            "时薪、每英里贡献、单笔贡献分别占 50%、35%、15%。",
            style = MaterialTheme.typography.bodySmall)
    }
}
