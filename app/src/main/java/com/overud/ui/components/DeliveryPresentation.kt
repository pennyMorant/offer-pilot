package com.overud.ui.components

import com.overud.data.database.projection.DeliverySummary
import com.overud.model.Platform
import java.util.Locale

fun deliveryCards(history: List<DeliverySummary>, selected: AnalysisSelection, byRegion: Boolean): List<DeliveryCardState> {
    val rows = history.filter { row ->
        (selected.platform == null || row.platform == selected.platform.name) &&
            (selected.region == null || row.regionKey == selected.region) &&
            (selected.weekday == null || row.weekday == selected.weekday) && row.localHour in selected.period.hours
    }
    val groups = rows.groupBy {
        listOf(it.platform, if (byRegion) it.regionKey else "ALL",
            if (byRegion) 0 else it.weekday, if (byRegion) 0 else it.localHour, it.zoneId, it.measurementVersion).joinToString("|")
    }
    return groups.toSortedMap().map { (key, group) ->
        val row = group.first()
        val completed = group.sumOf { it.completedCount }
        val interrupted = group.sumOf { it.interruptedCount }
        val measured = group.sumOf { it.measuredCount }
        val pairs = group.sumOf { it.pairedCount }
        val title = if (byRegion) regionLabel(row.regionKey)
            else "${weekdayLabel(row.weekday)} ${row.localHour}:00–${row.localHour + 1}:00"
        val platform = Platform.entries.find { it.name == row.platform }?.label ?: row.platform
        val duration = if (measured > 0) "平均观测耗时 ${minutes(group.sumOf { it.observedMillisSum }, measured)} 分钟"
            else "起点不明，暂无完整计时样本"
        val comparison = if (pairs > 0) {
            val observed = minutes(group.sumOf { it.pairedObservedMillisSum }, pairs)
            val quoted = minutes(group.sumOf { it.pairedQuotedMillisSum }, pairs)
            val longer = group.sumOf { it.longerThanQuoteCount }
            "配对样本 $pairs 次：报价平均 $quoted 分钟，观测平均 $observed 分钟；超过报价耗时 $longer 次"
        } else "暂无可与原报价配对比较的耗时样本"
        DeliveryCardState("delivery|$key", title, "$platform · ${row.zoneId} · 观测口径 v${row.measurementVersion}",
            "确认完成 $completed 次 · 中断观测 $interrupted 次 · 完整计时 $measured 次", duration, comparison)
    }
}

private fun minutes(sum: Long, count: Long) = String.format(Locale.getDefault(), "%.1f", sum / count.toDouble() / 60000)
