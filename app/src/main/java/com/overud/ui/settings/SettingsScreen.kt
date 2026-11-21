package com.overud.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overud.di.OverudViewModelFactory
import com.overud.ui.components.ToolSectionRow
import com.overud.ui.rules.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("appearance") {
            ToolSectionRow(Icons.Rounded.Palette, "外观",
                "${theme.mode.label} · ${if (theme.dynamic) "系统动态色" else "品牌配色"}", 0, 4, section == "theme") {
                section = if (section == "theme") null else "theme"
            }
        }
        item("appearance_content") {
            SettingsSection(section == "theme") { ThemeSettingsCard(theme, appearance::mode, appearance::dynamic) }
        }
        item("voice") {
            ToolSectionRow(Icons.Rounded.RecordVoiceOver, "驾驶语音", "订单提醒、播报内容与试听", 1, 4, section == "voice") {
                section = if (section == "voice") null else "voice"
            }
        }
        item("voice_content") { SettingsSection(section == "voice") { VoiceSettingsCard(voice) } }
        item("prop22") {
            ToolSectionRow(Icons.Rounded.AccountBalance, "Prop 22 估算", "最低时薪、小费假设与机动车", 2, 4, section == "prop22") {
                section = if (section == "prop22") null else "prop22"
            }
        }
        item("prop22_content") {
            SettingsSection(section == "prop22") {
                Prop22Fields(prop22, ruleState.saving, rules::updateProp22)
                ruleState.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                Button(onClick = rules::save, enabled = !ruleState.saving, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (ruleState.saving) "正在保存…" else "保存估算参数")
                }
            }
        }
        item("permissions") {
            ToolSectionRow(Icons.Rounded.Security, "连接与权限", "Shevery、通知、悬浮窗与定位", 3, 4, onClick = onPermissions)
        }
        item("developer") { DeveloperTools(factory) }
        item("version") {
            Text("OfferPilot 0.1.0 · Android 13–17", modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
