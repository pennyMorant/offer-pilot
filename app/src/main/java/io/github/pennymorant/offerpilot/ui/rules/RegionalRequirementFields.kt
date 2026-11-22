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

@Composable
fun RegionalRequirementFields(
    state: RulesUiState,
    onEnabled: (Boolean) -> Unit,
    onField: (RuleField, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                ToggleRow("要求目的地历史报价质量达标", "可选；开启后证据不足的报价不会自动接单",
                    state.regionalEnabled, enabled = !state.saving, onChange = onEnabled)
            }
        }
        AnimatedVisibility(
            visible = state.regionalEnabled,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField("最低历史优质报价比例（1–100）", state.regionalGoodPercent, "%",
                            { onField(RuleField.REGION_PERCENT, it) }, enabled = !state.saving, error = state.fieldErrors[RuleField.REGION_PERCENT])
                        NumberField("最低有效分类样本（10–100000）", state.regionalSamples, "张",
                            { onField(RuleField.REGION_SAMPLES, it) }, enabled = !state.saving, error = state.fieldErrors[RuleField.REGION_SAMPLES])
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "仅使用同平台、目的地所在网格的送达时段历史。需要地址或坐标精度和可信总耗时；解析中、样本不足或只剩地区整体基线时均不通过。历史优质比例不是下一单概率。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
}
