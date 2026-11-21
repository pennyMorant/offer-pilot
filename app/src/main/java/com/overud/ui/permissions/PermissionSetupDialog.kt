package com.overud.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PermissionSetupDialog(
    state: PermissionSetupState,
    onStart: () -> Unit,
    onRequest: (SetupPermission) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    if (!state.visible) return
    val current = state.current
    val summary = state.stage == SetupStage.SUMMARY
    val welcome = state.stage == SetupStage.WELCOME
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(20.dp).widthIn(max = 440.dp).fillMaxWidth().heightIn(max = 680.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                        Icon(Icons.Rounded.Explore, null, Modifier.padding(14.dp).size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(if (welcome) "让 OfferPilot 准备就绪" else if (summary) "准备情况已确认" else current?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall)
                    Text(if (welcome) "完成几项权限设置，让订单分析在跑单时及时出现。你可以逐项选择，也可以稍后补齐。"
                        else if (summary) "已开启 ${state.granted.size} / ${SetupPermission.entries.size} 项。授权不会开始工作，也不会开启自动接单。"
                        else current?.explanation.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    if (welcome || summary) PermissionChecklist(state)
                    else if (current != null) {
                        LinearProgressIndicator(progress = { (current.ordinal + 1f) / SetupPermission.entries.size },
                            modifier = Modifier.fillMaxWidth())
                        Text("设置 ${current.ordinal + 1} / ${SetupPermission.entries.size}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
                            Text(current.impact, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (current == SetupPermission.OVERLAY || current == SetupPermission.LISTENER) {
                            Text("下一步将打开系统设置，开启 OfferPilot 对应开关后返回即可。",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    state.message?.let {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                            Text(it, Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    if (summary && state.granted.size < SetupPermission.entries.size) {
                        Text("未开启的项目可在「设置 → 连接与权限」中继续设置。",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { if (welcome) onStart() else if (summary) onFinish() else current?.let(onRequest) },
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.busy) "等待授权结果…" else if (welcome) "开始设置" else if (summary) "进入 OfferPilot" else current?.action.orEmpty()) }
                    if (!summary) TextButton(
                        onClick = if (welcome) onFinish else onSkip,
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (welcome) "稍后再设置" else "跳过此项") }
                }
            }
        }
    }
}

@Composable
private fun PermissionChecklist(state: PermissionSetupState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SetupPermission.entries.forEach { step ->
            val granted = step in state.granted
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                Text(step.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(if (granted) "已开启" else if (state.stage == SetupStage.SUMMARY) "稍后设置" else "待授权",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
