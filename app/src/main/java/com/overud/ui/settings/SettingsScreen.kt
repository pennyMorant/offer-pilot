package com.overud.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overud.data.preferences.ThemeMode
import com.overud.di.OverudViewModelFactory
import com.overud.ui.components.ToggleRow
import com.overud.ui.rules.*

@Composable fun SettingsScreen(factory: OverudViewModelFactory, rules: RulesViewModel,
    voice: VoiceViewModel, onPermissions: () -> Unit) {
    val appearance: AppearanceViewModel = viewModel(factory = factory)
    val theme by appearance.state.collectAsStateWithLifecycle()
    val prop22 by rules.prop22.collectAsStateWithLifecycle()
    val ruleState by rules.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ListItem(headlineContent = { Text("外观") }, supportingContent = {
                Text("${theme.mode.label} · ${if (theme.dynamic) "系统动态色" else "默认配色"}")
            }, modifier = Modifier.clickable { section = if (section == "theme") null else "theme" })
        }
        if (section == "theme") item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth().clickable { appearance.mode(mode) }) {
                        RadioButton(theme.mode == mode, onClick = { appearance.mode(mode) })
                        Text(mode.label, Modifier.padding(top = 12.dp))
                    }
                }
                ToggleRow("使用系统动态色", "跟随壁纸配色，修改即时生效", theme.dynamic, onChange = appearance::dynamic)
            }
        }
        item { ListItem(headlineContent = { Text("驾驶语音") }, supportingContent = { Text("播报、音量与试听") },
            modifier = Modifier.clickable { section = if (section == "voice") null else "voice" }) }
        if (section == "voice") item { VoiceSettingsCard(voice) }
        item { ListItem(headlineContent = { Text("Prop 22 估算") }, supportingContent = { Text("最低时薪、小费假设与车辆类型") },
            modifier = Modifier.clickable { section = if (section == "prop22") null else "prop22" }) }
        if (section == "prop22") item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Prop22Fields(prop22, ruleState.saving, rules::updateProp22)
                ruleState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = rules::save, enabled = !ruleState.saving) { Text("保存估算参数") }
            }
        }
        item { ListItem(headlineContent = { Text("连接与权限") }, supportingContent = { Text("Shevery、悬浮窗、通知与位置") },
            modifier = Modifier.clickable(onClick = onPermissions)) }
        item { HorizontalDivider() }
        item { DeveloperTools(factory) }
        item { Text("Overud 0.1.0 · Android 13+", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
