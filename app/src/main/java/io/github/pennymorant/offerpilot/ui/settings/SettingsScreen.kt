@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pennymorant.offerpilot.data.preferences.ThemeMode
import io.github.pennymorant.offerpilot.di.OverudViewModelFactory
import io.github.pennymorant.offerpilot.ui.components.ToolSectionRow
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardUiState
import io.github.pennymorant.offerpilot.ui.diagnostics.DiagnosticsContent
import io.github.pennymorant.offerpilot.ui.diagnostics.RuntimeDiagnosticsViewModel
import io.github.pennymorant.offerpilot.ui.rules.RulesViewModel
import io.github.pennymorant.offerpilot.ui.rules.VoiceViewModel

@Composable
fun SettingsScreen(
    factory: OverudViewModelFactory,
    rules: RulesViewModel,
    voice: VoiceViewModel,
    diagnostics: RuntimeDiagnosticsViewModel,
    dashboardState: DashboardUiState,
    section: String?,
    onSection: (String?) -> Unit,
    onAutomatic: (Boolean) -> Unit,
    onOverlayPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onLocationPermission: () -> Unit,
) {
    val appearance: AppearanceViewModel = viewModel(factory = factory)
    val theme by appearance.state.collectAsStateWithLifecycle()
    val prop22 by rules.prop22.collectAsStateWithLifecycle()
    val ruleState by rules.state.collectAsStateWithLifecycle()
    val voiceSettings by voice.settings.collectAsStateWithLifecycle()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    AnimatedContent(
        targetState = section,
        transitionSpec = { fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec) },
        label = "settingsSection",
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
    ) { currentSection ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 10.dp,
                bottom = if (currentSection == null) 104.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (currentSection == null) {
                item("brand") { SettingsBrandCard() }
                item("display_mode") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("显示模式", style = MaterialTheme.typography.titleMedium)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                ThemeMode.entries.forEachIndexed { index, mode ->
                                    val shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        ThemeMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = theme.mode == mode,
                                        onCheckedChange = { appearance.mode(mode) },
                                        shapes = shapes,
                                        modifier = Modifier.weight(1f),
                                    ) { Text(mode.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                                }
                            }
                        }
                    }
                }
                item("entries") {
                    val count = if (hasDeveloperTools) 5 else 4
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ToolSectionRow(
                            Icons.Rounded.RecordVoiceOver, "驾驶语音",
                            if (voiceSettings.enabled) "已开启 · 实时播报订单与配送状态" else "已关闭 · 点击配置语音与试听",
                            0, count, onClick = { onSection("驾驶语音") },
                        )
                        ToolSectionRow(
                            Icons.Rounded.AccountBalance, "Prop 22 估算",
                            if (prop22.enabled) "已开启 · 最低时薪 $${prop22.wage}/h" else "未启用 · 加州保底估算参考",
                            1, count, onClick = { onSection("Prop 22 估算") },
                        )
                        ToolSectionRow(
                            Icons.Rounded.Bolt, "自动接单（实验）",
                            if (dashboardState.autoAcceptEnabled) "已开启 · 满足全部条件时执行" else "已关闭 · 降低平台风控风险",
                            2, count, onClick = { onSection("自动接单（实验）") },
                        )
                        ToolSectionRow(
                            Icons.Rounded.Security, "连接与权限",
                            "Shevery、通知、悬浮窗与定位状态",
                            3, count, onClick = { onSection("连接与权限") },
                        )
                        if (hasDeveloperTools) {
                            ToolSectionRow(
                                Icons.Rounded.BugReport, "开发工具",
                                "图片识别、节点诊断、规则调试与模拟弹窗",
                                4, count, onClick = { onSection("开发工具") },
                            )
                        }
                    }
                }
            } else {
                item("sub_content") {
                    when (currentSection) {
                        "驾驶语音" -> VoiceSettingsPage(voice)
                        "Prop 22 估算" -> Prop22SettingsPage(prop22, ruleState.saving, ruleState.error, rules::updateProp22, rules::save)
                        "自动接单（实验）" -> AutoAcceptSettingsPage(dashboardState, onAutomatic)
                        "连接与权限" -> DiagnosticsContent(diagnostics, onOverlayPermission, onNotificationPermission, onLocationPermission)
                        "开发工具" -> DeveloperTools(factory)
                    }
                }
            }
        }
    }
}
