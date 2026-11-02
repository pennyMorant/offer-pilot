package com.overud.ui.regions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.*

@Composable
fun RegionsScreen(state: RegionsUiState) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageTitle("KNOW YOUR WORKING AREAS", "地区质量，看长期。", "最近 90 天 · 按收到报价时的位置网格汇总。") }
        item { EmptyStateCard("个人报价统计", "未知位置单独归组。这里比较已观察报价的质量，不代表平台全部订单或真实派单频率。") }
        if (state.regions.isEmpty()) item { EmptyStateCard("地区样本不足", "开始工作并授权定位后，报价会归入相应网格；缺失位置时仍记录其他统计。") }
        items(state.regions, key = { it.id }) { QualityCard(it) }
    }
}
