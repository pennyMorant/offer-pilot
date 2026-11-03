package com.overud.ui.rules

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.model.Platform
import com.overud.ui.components.*

@Composable fun RulesScreen(viewModel: RulesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.error) {
        if (state.error != null) section = when (state.fieldErrors.keys.firstOrNull()) {
            RuleField.MINIMUM, RuleField.MILE, RuleField.HOUR, RuleField.COST,
            RuleField.MINIMUM_CONTRIBUTION -> "最低收益"
            RuleField.TARGET_CONTRIBUTION, RuleField.TARGET_CONTRIBUTION_MILE,
            RuleField.TARGET_CONTRIBUTION_HOUR, RuleField.MINIMUM_SCORE -> "价值评分"
            RuleField.DISTANCE, RuleField.TIME, RuleField.PICKUP -> "距离与时间"
            RuleField.REGION_PERCENT, RuleField.REGION_SAMPLES -> "目的地区域"
            else -> section
        }
    }
    BackHandler(section != null) { section = null }
    Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { RuleScopeSelector(state, viewModel::selectScope, viewModel::inheritShared) }
            if (section != null) item {
                TextButton(onClick = { section = null }) { Text("返回规则概览") }
                Text(section.orEmpty(), style = MaterialTheme.typography.titleLarge)
            }
            if (section == null) {
                item { Text(if (viewModel.hasChanges()) "当前草稿" else "已保存要求", style = MaterialTheme.typography.titleMedium) }
                item { RuleGroupRow("平台与范围", state.platforms.joinToString { it.label }) { section = "平台与范围" } }
                if (state.scope != null && state.inheritShared) item {
                    Text("此平台继承公共要求。关闭继承后可单独编辑。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    item { RuleGroupRow("最低收益", state.incomeSummary()) { section = "最低收益" } }
                    item { RuleGroupRow("价值评分", state.scoreSummary()) { section = "价值评分" } }
                    item { RuleGroupRow("距离与时间", state.routeSummary()) { section = "距离与时间" } }
                    item { RuleGroupRow("目的地区域", if (state.regionalEnabled) "优质占比 ${state.regionalGoodPercent}% · 至少 ${state.regionalSamples} 个样本" else "未启用") { section = "目的地区域" } }
                    item { RuleGroupRow("订单偏好", "叠单、追加单及排除条件") { section = "订单偏好" } }
                }
                item { Text("以上为当前草稿。开启的条件须同时满足；保存后自动接单保持关闭。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (section == "平台与范围") {
                item { Column { Platform.entries.forEach { platform ->
                    ToggleRow(platform.label, "使用已保存要求", platform in state.platforms, enabled = !state.saving) {
                        viewModel.platform(platform, it)
                    }
                } } }
            } else if (state.scope != null && state.inheritShared) item { Text("此平台跟随公共要求，请切换至公共要求编辑。") }
            else item {
                when (section) {
                    "最低收益" -> EarningsFields(state, viewModel::edit, viewModel::condition, viewModel::hourlyBasis)
                    "价值评分" -> ValueScoreFields(state, viewModel::edit)
                    "距离与时间" -> DistanceTimeFields(state, viewModel::edit, viewModel::condition)
                    "目的地区域" -> RegionalRequirementFields(state, viewModel::regional, viewModel::edit)
                    "订单偏好" -> PlatformPreferences(state, viewModel)
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(if (viewModel.hasChanges()) "有未保存修改" else "已保存", style = MaterialTheme.typography.labelMedium)
                Button(onClick = viewModel::save, enabled = !state.saving && viewModel.hasChanges(),
                    modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "正在保存…" else "保存接单要求") }
            }
        }
    }
}

@Composable private fun RuleGroupRow(title: String, detail: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(detail) }, trailingContent = { Text("›") },
        modifier = Modifier.clickable(onClick = onClick))
}
