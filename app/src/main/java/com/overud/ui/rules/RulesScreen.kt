@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.model.Platform
import com.overud.ui.components.*

@Composable fun RulesScreen(
    viewModel: RulesViewModel,
    onOverlayPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onLocationPermission: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("YOUR DRIVE, YOUR RULES", "接什么单，你来定。", "所有要求同时满足才自动接单。保存后重新开启自动接单。") }
        item { SectionLabel("接单平台") }
        items(Platform.entries) { platform -> ToggleRow(platform.label, "使用以下接单要求", platform in state.platforms) {
            viewModel.platform(platform, it)
        } }
        item { SectionLabel("最低收益 · USD") }
        item { NumberField("最低报价", state.minimum, "$", { viewModel.edit(RuleField.MINIMUM, it) }) }
        item { NumberField("最低每英里报价", state.mile, "$/mi", { viewModel.edit(RuleField.MILE, it) }) }
        item { NumberField("最低预计毛时薪", state.hour, "$/h", { viewModel.edit(RuleField.HOUR, it) }) }
        item { SectionLabel("距离与时间") }
        item { NumberField("最大总里程", state.distance, "mi", { viewModel.edit(RuleField.DISTANCE, it) }) }
        item { NumberField("最长订单耗时", state.time, "min", { viewModel.edit(RuleField.TIME, it) }) }
        item { NumberField("最大取餐距离（留空不限制）", state.pickup, "mi", { viewModel.edit(RuleField.PICKUP, it) }) }
        item { NumberField("车辆成本（用于贡献估计）", state.cost, "$/mi", { viewModel.edit(RuleField.COST, it) }) }
        item { SectionLabel("额外偏好") }
        item { ToggleRow("允许叠单", "按整组报价与路线评估", state.stacked) { viewModel.stacked(it) } }
        item { ToggleRow("允许追加单", "仅在新增收入及增量路线完整时执行", state.addon) { viewModel.addon(it) } }
        item { OutlinedTextField(value = state.excluded, onValueChange = { viewModel.edit(RuleField.EXCLUDED, it) }, label = { Text("排除目的地关键词，用逗号分隔") }, modifier = Modifier.fillMaxWidth()) }
        item {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("保存接单要求") }
        }
        item { SectionLabel("设备权限") }
        item { FilledTonalButton(onClick = onOverlayPermission, modifier = Modifier.fillMaxWidth()) { Text("设置悬浮窗权限") } }
        item { OutlinedButton(onClick = onNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("设置通知读取权限") } }
        item { OutlinedButton(onClick = onLocationPermission, modifier = Modifier.fillMaxWidth()) { Text("授权位置记录（下次开始工作生效）") } }
        item { Text("Overud 0.1.0 · Material 3 Expressive", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
