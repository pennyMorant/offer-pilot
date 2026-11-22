@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.ui.components.ToggleRow
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardUiState
import io.github.pennymorant.offerpilot.ui.rules.VoiceViewModel

@Composable
fun VoiceSettingsPage(viewModel: VoiceViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bg by animateColorAsState(
        targetValue = if (settings.enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "voiceHeroBg",
    )
    val fg = if (settings.enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleRow("驾驶语音播报", "开工后自动播报新报价与配送状态", settings.enabled, onChange = viewModel::enabled)
                ToolBadge(text = status)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(0, 2),
                modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    ToggleRow("播报 Prop 22 情景补差", "仅在启用估算且字段足够时播报，不代表实际到账",
                        settings.includeProp22, onChange = viewModel::prop22)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(1, 2),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = viewModel::preview, shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).height(52.dp)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("试听中文")
                        }
                        OutlinedButton(onClick = viewModel::stop, shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).height(52.dp)) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("停止播报")
                        }
                    }
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                                .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
                        },
                    ) {
                        Text("系统文字转语音设置")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()) {
            Text("需要系统离线中文语音，音量与输出设备遵循系统设置，不会强制打开扬声器。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp))
        }
    }
}

@Composable
fun AutoAcceptSettingsPage(state: DashboardUiState, onAutomatic: (Boolean) -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (state.autoAcceptEnabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "autoHeroBg",
    )
    val fg = if (state.autoAcceptEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val canToggle = state.autoAcceptEnabled || (state.working && state.connection.serviceReady &&
        !state.pending && !state.policySaving)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleRow("启用自动接单", "需先开启工作；满足全部启用条件时自动点击",
                    state.autoAcceptEnabled, enabled = canToggle, onChange = onAutomatic)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolBadge(text = when {
                        state.automatic -> "运行中"
                        state.autoAcceptEnabled -> "待命"
                        else -> "已关闭"
                    })
                    if (!state.working) {
                        Text("请先在工作台开启工作", style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.78f))
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("风控与安全边界", style = MaterialTheme.typography.titleMedium)
                Text("• 自动点击接单存在触发平台风控检测的风险，默认保持关闭。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• 结束工作、应用重启、修改规则或出现异常时会自动关闭此开关。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• 日常跑单建议仅使用悬浮分析卡与驾驶语音播报，由本人手动确认接单。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
