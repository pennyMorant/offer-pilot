@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.ToolBadge

@Composable
fun RulesScreen(
    viewModel: RulesViewModel,
    section: String?,
    onSection: (String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.error) {
        if (state.error != null) onSection(when (state.fieldErrors.keys.firstOrNull()) {
            RuleField.MINIMUM, RuleField.MILE, RuleField.HOUR, RuleField.COST,
            RuleField.MINIMUM_CONTRIBUTION -> "最低收益"
            RuleField.TARGET_CONTRIBUTION, RuleField.TARGET_CONTRIBUTION_MILE,
            RuleField.TARGET_CONTRIBUTION_HOUR, RuleField.MINIMUM_SCORE -> "价值评分"
            RuleField.DISTANCE, RuleField.TIME, RuleField.PICKUP -> "距离与时间"
            RuleField.REGION_PERCENT, RuleField.REGION_SAMPLES -> "目的地区域"
            RuleField.MAX_SHOP_ITEMS, RuleField.RADAR_EXTRA_SCORE -> "订单偏好"
            else -> section
        })
    }
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
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 10.dp,
                    bottom = if (currentSection == null) 104.dp else 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentSection == null) {
                    item { RuleScopeSelector(state, viewModel::selectScope, viewModel::inheritShared) }
                    item { RuleOverviewHero(state, dirty, viewModel::save, viewModel::discardChanges) }
                    item { RuleBentoGrid(state) { onSection(it) } }
                    item {
                        Text("开启的条件须同时满足；保存后自动接单保持关闭。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    if (currentSection == "平台与范围") {
                        item { PlatformToggleCards(state, viewModel::platform) }
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
            visible = section != null && (dirty || state.error != null || state.saving),
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = viewModel::discardChanges,
                            enabled = !state.saving && dirty,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(0.9f).height(52.dp),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("放弃")
                        }
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.saving && dirty,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1.4f).height(52.dp),
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.saving) "正在保存…" else "保存接单要求", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformToggleCards(state: RulesUiState, onToggle: (Platform, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Platform.entries.forEach { platform ->
            val enabled = platform in state.platforms
            val bg by animateColorAsState(
                targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "platBg",
            )
            val fg = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            Surface(
                onClick = { onToggle(platform, !enabled) },
                enabled = !state.saving,
                color = bg,
                contentColor = fg,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f).heightIn(min = 112.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(platform.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    Text(if (enabled) "已启用自动评估" else "已暂停该平台",
                        style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.78f))
                }
            }
        }
    }
}
