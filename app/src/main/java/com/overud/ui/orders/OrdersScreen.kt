package com.overud.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.*

@Composable
fun OrdersScreen(cards: List<QualityCardState>) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageTitle("QUALITY THROUGH THE DAY", "好时段，数据说话。", "最近 90 天 · 按平台、星期与当地小时汇总，不保存逐单明细。") }
        item { EmptyStateCard("质量标准 v1", "优质：报价 ≥ $8、每英里 ≥ $2、预估毛时薪 ≥ $25，三项同时满足。质量评价独立于自动接单开关。") }
        if (cards.isEmpty()) item { EmptyStateCard("还没有统计样本", "工作期间识别到的报价会归入统计分组，包括未接的报价。") }
        items(cards, key = { it.id }) { QualityCard(it) }
    }
}
