@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.ToggleRow
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import io.github.pennymorant.offerpilot.ui.components.ToolSectionRow
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun RulesScreen(viewModel: RulesViewModel) {
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
            RuleField.MAX_SHOP_ITEMS, RuleField.RADAR_EXTRA_SCORE -> "订单偏好"
            else -> section
        }
    }
    BackHandler(section != null) { section = null }
    val dirty = viewModel.hasChanges()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
        AnimatedContent(
            targetState = section,
            transitionSpec = { fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec) },
            label = "rulesSection",
            modifier = Modifier.weight(1f),
        ) { currentSection ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentSection == null) {
                    item { RuleScopeSelector(state, viewModel::selectScope, viewModel::inheritShared) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToolBadge(
                                text = if (dirty) "草稿未保存" else "已保存规则",
                                containerColor = if (dirty) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (dirty) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text("开启的条件须同时满足", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        val inheriting = state.scope != null && state.inheritShared
                        val rows = buildList {
                            add(Triple(Icons.Rounded.Apps, "平台与范围", state.platforms.joinToString { it.label }))
                            if (!inheriting) {
                                add(Triple(Icons.Rounded.AttachMoney, "最低收益", state.incomeSummary()))
                                add(Triple(Icons.Rounded.StarRate, "价值评分", state.scoreSummary()))
                                add(Triple(Icons.Rounded.Route, "距离与时间", state.routeSummary()))
                                add(Triple(Icons.Rounded.LocationOn, "目的地区域",
                                    if (state.regionalEnabled) "优质占比 ${state.regionalGoodPercent}% · 至少 ${state.regionalSamples} 样本" else "未启用"))
                                add(Triple(Icons.Rounded.FilterList, "订单偏好", state.preferencesSummary()))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            rows.forEachIndexed { idx, (icon, title, detail) ->
                                ToolSectionRow(icon, title, detail, idx, rows.size) { section = title }
                            }
                        }
                    }
                    if (state.scope != null && state.inheritShared) item {
                        Text("此平台继承公共要求。关闭继承后可单独编辑。", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        Text("保存后自动接单保持关闭，需手动确认开启。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalIconButton(
                                onClick = { section = null },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", modifier = Modifier.size(20.dp))
                            }
                            Text(currentSection, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            ToolBadge(text = state.scope?.label ?: "公共要求")
                        }
                    }
                    if (currentSection == "平台与范围") {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Platform.entries.forEachIndexed { idx, platform ->
                                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = groupedItemShape(idx, Platform.entries.size), modifier = Modifier.fillMaxWidth()) {
                                        Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                                            ToggleRow(platform.label, "使用已保存要求", platform in state.platforms, enabled = !state.saving) {
                                                viewModel.platform(platform, it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (state.scope != null && state.inheritShared) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("此平台跟随公共要求，请返回关闭继承或切换至公共要求编辑。",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp))
                            }
                        }
                    } else item {
                        when (currentSection) {
                            "最低收益" -> EarningsFields(state, viewModel::edit, viewModel::condition, viewModel::hourlyBasis)
                            "价值评分" -> ValueScoreFields(state, viewModel::edit)
                            "距离与时间" -> DistanceTimeFields(state, viewModel::edit, viewModel::condition)
                            "目的地区域" -> RegionalRequirementFields(state, viewModel::regional, viewModel::edit)
                            "订单偏好" -> PlatformPreferences(state, viewModel)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = dirty || state.error != null || state.saving,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.saving && dirty,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.saving) "正在保存…" else "保存接单要求", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
