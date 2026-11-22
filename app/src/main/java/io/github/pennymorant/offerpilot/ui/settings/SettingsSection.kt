package io.github.pennymorant.offerpilot.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.data.preferences.AppearanceSettings
import io.github.pennymorant.offerpilot.data.preferences.ThemeMode
import io.github.pennymorant.offerpilot.ui.components.ToggleRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSection(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
fun ThemeSettingsCard(theme: AppearanceSettings, onMode: (ThemeMode) -> Unit, onDynamic: (Boolean) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("显示模式", style = MaterialTheme.typography.titleSmall)
        ThemeMode.entries.forEach { mode ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .selectable(selected = theme.mode == mode, role = Role.RadioButton, onClick = { onMode(mode) })
                .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected = theme.mode == mode, onClick = null)
                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    ToggleRow("使用系统动态色", "根据壁纸生成应用配色，开关立即保存", theme.dynamic, onChange = onDynamic)
}
