@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.ToolBadge

@Composable
fun RuleBentoGrid(
    state: RulesUiState,
    onTogglePlatform: (Platform, Boolean) -> Unit,
    onOpen: (String) -> Unit,
) {
    val inheriting = state.scope != null && state.inheritShared
    val scoreNum = state.minimumScore.trim().toIntOrNull()?.coerceIn(0, 100) ?: 70
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Platform.entries.forEach { platform ->
                val enabled = platform in state.platforms
                val bg by animateColorAsState(
                    targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    label = "inlinePlatBg",
                )
                val fg = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                Surface(
                    onClick = { onTogglePlatform(platform, !enabled) },
                    enabled = !state.saving,
                    color = bg,
                    contentColor = fg,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(platform.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (enabled) "已启用" else "已关闭", style = MaterialTheme.typography.bodySmall,
                                color = fg.copy(alpha = 0.78f))
                        }
                        Icon(if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        if (!inheriting) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BentoTile(
                    icon = Icons.Rounded.AttachMoney,
                    title = "收益底线",
                    headline = "$${state.minimum.ifBlank { "—" }} · $${state.mile.ifBlank { "—" }}/mi",
                    detail = "$${state.hour.ifBlank { "—" }}/h · 贡献$${state.minimumContribution.ifBlank { "—" }}",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("最低收益") },
                )
                BentoTile(
                    icon = Icons.Rounded.StarRate,
                    title = "价值评分",
                    headline = "${scoreNum}分",
                    detail = "目标 $${state.targetContributionHour.ifBlank { "—" }}/h",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("价值评分") },
                ) {
                    LinearWavyProgressIndicator(
                        progress = { scoreNum / 100f },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BentoTile(
                    icon = Icons.Rounded.Route,
                    title = "距离与时间",
                    headline = "${state.distance.ifBlank { "—" }}mi · ${state.time.ifBlank { "—" }}m",
                    detail = if (state.pickup.isNotBlank()) "取餐 ≤${state.pickup}mi" else "不限取餐距离",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("距离与时间") },
                )
                BentoTile(
                    icon = Icons.Rounded.LocationOn,
                    title = "目的地区域",
                    headline = if (state.regionalEnabled) "${state.regionalGoodPercent}%" else "未启用",
                    detail = if (state.regionalEnabled) "至少 ${state.regionalSamples} 样本" else "点击配置区域门槛",
                    containerColor = if (state.regionalEnabled) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = if (state.regionalEnabled) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("目的地区域") },
                )
            }
            WideRuleRow(
                icon = Icons.Rounded.Tune,
                title = "订单偏好与过滤",
                index = 0,
                count = 1,
                onClick = { onOpen("订单偏好") },
            ) {
                val tags = buildList {
                    if (state.allowTripRadar) add("雷达单")
                    if (state.allowShopAndPay) add("商超")
                    if (state.allowPackage) add("包裹")
                    if (state.stacked) add("叠单")
                    if (state.addon) add("追加")
                }
                val kwCount = listOf(state.excluded, state.excludedMerchants, state.preferredZones)
                    .sumOf { s -> s.split(',', '，').count { it.trim().isNotEmpty() } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { ToolBadge(text = it) }
                    if (kwCount > 0) ToolBadge(text = "$kwCount 个关键词")
                }
            }
        }
    }
}
