package com.overud.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.overud.analytics.LoggedOfferItem
import com.overud.analytics.OrderQuality
import com.overud.model.OfferChannel
import com.overud.model.Platform
import com.overud.ui.components.ToolBadge
import com.overud.ui.components.ToolMetricTile
import com.overud.ui.components.groupedItemShape
import com.overud.ui.components.money
import com.overud.valuation.ValueEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyReviewPage(
    state: DailyReviewUiState,
    onRange: (DailyDateRange) -> Unit,
    onPlatform: (Platform?) -> Unit,
    onFilter: (DailyItemFilter) -> Unit,
    onClearAll: () -> Unit,
) {
    val summary = state.summary
    var confirmClear by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DailyDateRange.entries.forEach { item ->
                    FilterChip(selected = state.range == item, onClick = { onRange(item) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelLarge) })
                }
                FilterChip(selected = state.platform == null, onClick = { onPlatform(null) },
                    label = { Text("全部平台", style = MaterialTheme.typography.labelLarge) })
                Platform.entries.forEach { p ->
                    FilterChip(selected = state.platform == p, onClick = { onPlatform(p) },
                        label = { Text(p.label, style = MaterialTheme.typography.labelLarge) })
                }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("推送总量", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.totalCount} 单", style = MaterialTheme.typography.displayMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("好单 ${summary.goodPercent}% · 垃圾单 ${summary.lowPercent}%",
                                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            if (summary.acceptedCount > 0) {
                                Text("已接 ${summary.acceptedCount}单 (${money(summary.acceptedAmountCents)}) · ${summary.acceptedPercent}%",
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    QualityRatioBar(summary.goodCount, summary.normalCount, summary.lowCount)
                    Text(
                        "好单 ${summary.goodCount} (${summary.goodPercent}%) · 普通 ${summary.normalCount} (${summary.normalPercent}%) · " +
                            "垃圾 ${summary.lowCount} (${summary.lowPercent}%)" +
                            if (summary.unknownCount > 0) " · 未知 ${summary.unknownCount}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolMetricTile("均每英里", summary.avgCentsPerMile?.let(::money) ?: "—", Modifier.weight(1f))
                        ToolMetricTile("均毛时薪", summary.avgGrossCentsPerHour?.let(::money) ?: "—", Modifier.weight(1f))
                        ToolMetricTile("达标率", if (summary.totalCount > 0) "${summary.eligiblePercent}%" else "—", Modifier.weight(0.8f))
                    }
                    if (summary.totalCount > 0) HourlyVolumeStrip(summary.hourlyCounts)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ToolBadge("雷达 ${summary.radarCount}")
                            ToolBadge("专属 ${summary.exclusiveCount}")
                            if (summary.fakePreferredCount > 0) {
                                ToolBadge("假好单 ${summary.fakePreferredCount}",
                                    MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                            }
                            if (summary.shopCount > 0) ToolBadge("商超 ${summary.shopCount}")
                        }
                        if (summary.totalCount > 0) {
                            TextButton(onClick = { confirmClear = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text("清空", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DailyItemFilter.entries.forEach { f ->
                    FilterChip(selected = state.filter == f, onClick = { onFilter(f) },
                        label = { Text(f.label, style = MaterialTheme.typography.labelMedium) })
                }
            }
        }
        if (state.items.isEmpty()) {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "该范围暂无推送记录。开启工作后，系统会自动记录每笔观察到的报价指标。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            itemsIndexed(state.items, key = { _, item -> item.offerId }) { idx, item ->
                LoggedOfferRow(item, idx, state.items.size)
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空推送历史") },
            text = { Text("确定删除所有已记录的推送订单历史吗？区域和时段汇总统计不受影响。") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("确认清空") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun QualityRatioBar(good: Int, normal: Int, low: Int) {
    val total = (good + normal + low).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        if (good > 0) Box(Modifier.weight(good.toFloat() / total).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        if (normal > 0) Box(Modifier.weight(normal.toFloat() / total).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
        if (low > 0) Box(Modifier.weight(low.toFloat() / total).fillMaxHeight().background(MaterialTheme.colorScheme.error))
    }
}

@Composable
private fun HourlyVolumeStrip(counts: List<Int>) {
    val peak = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("24 小时推单分布（峰值 $peak 单）", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom) {
            counts.forEach { count ->
                val frac = (count.toFloat() / peak).coerceIn(0.08f, 1f)
                Box(Modifier.weight(1f).fillMaxHeight(if (count == 0) 0.08f else frac)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (count == 0) MaterialTheme.colorScheme.surfaceContainerHighest
                        else MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun LoggedOfferRow(item: LoggedOfferItem, index: Int, count: Int) {
    val locale = LocalConfiguration.current.locales[0]
    val timeText = SimpleDateFormat("MM-dd HH:mm", locale).format(Date(item.observedAt))
    val miles = if (item.distanceMeters > 0) String.format(Locale.US, "%.1f mi", item.distanceMeters / ValueEngine.METERS_PER_MILE) else "—"
    val mins = if (item.durationSeconds > 0) "${item.durationSeconds / 60} min" else "—"
    val (qLabel, qBg, qFg) = when (item.quality) {
        OrderQuality.GOOD -> Triple("好单", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        OrderQuality.LOW -> Triple("垃圾单", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        OrderQuality.NORMAL -> Triple("普通", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        OrderQuality.UNKNOWN -> Triple("未知", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = groupedItemShape(index, count),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(money(item.amountCents), style = MaterialTheme.typography.titleMedium)
                    ToolBadge(item.platform.label)
                    ToolBadge(qLabel, qBg, qFg)
                    if (item.accepted) ToolBadge("已接", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                    if (item.fakePreferred) ToolBadge("假好单", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    else if (item.channel == OfferChannel.TRIP_RADAR) ToolBadge("雷达")
                }
                Text(timeText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "$miles · $mins · ${if (item.centsPerMile > 0) "${money(item.centsPerMile)}/mi" else "—"} · " +
                    if (item.valueScore >= 0) "${item.valueScore}分" else "未评分",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.merchant.isNotBlank() || item.destinationArea.isNotBlank()) {
                Text(listOf(item.merchant, item.destinationArea).filter(String::isNotBlank).joinToString(" → "),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
