package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.overud.model.Platform
import com.overud.ui.components.ToggleRow

@Composable
fun RuleScopeSelector(state: RulesUiState, onScope: (Platform?) -> Unit, onInherit: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.scope == null, onClick = { onScope(null) },
                label = { Text("公共要求") }, enabled = !state.saving)
            Platform.entries.forEach { platform ->
                FilterChip(selected = state.scope == platform, onClick = { onScope(platform) },
                    label = { Text(platform.label) }, enabled = !state.saving)
            }
        }
        if (state.scope != null) {
            ToggleRow("跟随公共要求", "关闭后可为此平台单独设置条件", state.inheritShared,
                enabled = !state.saving, onChange = onInherit)
        }
        Text("切换范围会保留草稿；保存时统一写入所有修改。", style = MaterialTheme.typography.bodySmall)
    }
}
