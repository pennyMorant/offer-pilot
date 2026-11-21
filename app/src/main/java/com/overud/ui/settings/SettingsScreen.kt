package com.overud.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overud.data.preferences.ThemeMode
import com.overud.di.OverudViewModelFactory
import com.overud.ui.components.ToggleRow
import com.overud.ui.components.ToolSectionRow
import com.overud.ui.rules.*

@Composable
fun SettingsScreen(
    factory: OverudViewModelFactory,
    rules: RulesViewModel,
    voice: VoiceViewModel,
    onPermissions: () -> Unit,
) {
    val appearance: AppearanceViewModel = viewModel(factory = factory)
    val theme by appearance.state.collectAsStateWithLifecycle()
    val prop22 by rules.prop22.collectAsStateWithLifecycle()
    val ruleState by rules.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ToolSectionRow(Icons.Rounded.Palette, "外观",
                    "${theme.mode.label} · ${if (theme.dynamic) "系统动态色" else "工具默认色"}", 0, 4) {
                    section = if (section == "theme") null else "theme"
                }
                ToolSectionRow(Icons.Rounded.RecordVoiceOver, "驾驶语音", "假好单警示、雷达单与双开提醒", 1, 4) {
                    section = if (section == "voice") null else "voice"
                }
                ToolSectionRow(Icons.Rounded.AccountBalance, "Prop 22 估算", "取餐地最低时薪、小费假设与机动车", 2, 4) {
                    section = if (section == "prop22") null else "prop22"
                }
                ToolSectionRow(Icons.Rounded.Security, "连接与权限", "Shevery 无线调试、悬浮窗、通知与定位", 3, 4, onPermissions)
            }
        }
        item {
            AnimatedVisibility(visible = section == "theme") {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            Row(Modifier.fillMaxWidth().clickable { appearance.mode(mode) },
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(theme.mode == mode, onClick = { appearance.mode(mode) })
                                Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        ToggleRow("使用系统动态色", "跟随 Android 17 壁纸取色", theme.dynamic, onChange = appearance::dynamic)
                    }
                }
            }
        }
        item { AnimatedVisibility(visible = section == "voice") { VoiceSettingsCard(voice) } }
        item {
            AnimatedVisibility(visible = section == "prop22") {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Prop22Fields(prop22, ruleState.saving, rules::updateProp22)
                        ruleState.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        Button(onClick = rules::save, enabled = !ruleState.saving, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()) { Text("保存估算参数") }
                    }
                }
            }
        }
        item { DeveloperTools(factory) }
        item {
            Text("OfferPilot 0.1.0 · Android 13–17 M3 Expressive", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
