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

@Composable
fun RulesScreen(
    viewModel: RulesViewModel,
    onOverlayPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onLocationPermission: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prop22 by viewModel.prop22.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageTitle("YOUR DRIVE, YOUR RULES", "接什么单，你来定。", "开启的条件全部满足才接单；至少保留一项最低收益要求。") }
        item { SectionLabel("编辑范围") }
        item { RuleScopeSelector(state, viewModel::selectScope, viewModel::inheritShared) }
        item { SectionLabel("自动接单平台") }
        items(Platform.entries) { platform ->
            ToggleRow(platform.label, "开启后按该平台的已保存要求执行", platform in state.platforms,
                enabled = !state.saving) { viewModel.platform(platform, it) }
        }
        if (state.scope != null && state.inheritShared) {
            item { EmptyStateCard("此平台跟随公共要求", "公共要求保存后会同时应用于所有跟随它的平台。") }
        } else {
            item { SectionLabel("最低收益 · USD") }
            item { EarningsFields(state, viewModel::edit, viewModel::condition, viewModel::hourlyBasis) }
            item { SectionLabel("距离与时间") }
            item { DistanceTimeFields(state, viewModel::edit, viewModel::condition) }
            item { SectionLabel("目的地区域要求") }
            item { RegionalRequirementFields(state, viewModel::regional, viewModel::edit) }
            item { SectionLabel("额外偏好") }
            item { PlatformPreferences(state, viewModel) }
        }
        item { SectionLabel("Prop 22 额外收入估算 · 全局设置") }
        item { Prop22Fields(prop22, state.saving, viewModel::updateProp22) }
        item {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(if (state.saving) "正在保存…" else "保存全部接单要求") }
        }
        item { SectionLabel("设备权限") }
        item { FilledTonalButton(onClick = onOverlayPermission, modifier = Modifier.fillMaxWidth()) { Text("设置悬浮窗权限") } }
        item { OutlinedButton(onClick = onNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("设置通知读取权限") } }
        item { OutlinedButton(onClick = onLocationPermission, modifier = Modifier.fillMaxWidth()) { Text("授权位置记录（下次开始工作生效）") } }
        item { Text("Overud 0.1.0 · Material 3 Expressive", style = MaterialTheme.typography.labelMedium) }
    }
}
