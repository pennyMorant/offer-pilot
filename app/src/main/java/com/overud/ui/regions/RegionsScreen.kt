@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.regions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.overud.ui.components.*

@Composable fun RegionsScreen(state: RegionsUiState) {
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("KNOW YOUR NEXT STOP", "好区域，慢慢发现。", "从目的地报价记录开始，积累你的区域经验。") }
        item { EmptyStateCard("区域经验 · 最近 28 天", "按位置网格记录报价和采集覆盖时长。覆盖时长仅包含目标平台在前台、定位精度足够且无待核对接单的时间，是可接单时长的代理。") }
        items(state.regions, key = { "${it.key}:${it.platformLabel}" }) { region ->
            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("网格 ${region.key}", style = MaterialTheme.typography.titleLarge)
                Text("${region.platformLabel} · ${region.offerCount} 张报价")
                Text("${region.eligibleCount} 张符合当时规则 · 覆盖 ${region.coverageMinutes} 分钟")
                Text(if (region.observedRatePerHour != null) "观测报价 ${"%.1f".format(region.observedRatePerHour)} 张/覆盖小时" else "样本不足，暂不推断接单机会", style = MaterialTheme.typography.bodySmall)
            } }
        }
        if (state.destinations.isEmpty()) item { EmptyStateCard("目的地记录不足", "有明确目的地的报价会汇总在这里，缺失地址不会被推断成精确位置。") }
        state.destinations.forEach { destination -> item {
            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(destination.name, style = MaterialTheme.typography.titleLarge)
                Text("${destination.offerCount} 张报价 · ${destination.eligibleCount} 张符合当时规则")
                Text("个人报价样本 · 尚无等待时间统计", style = MaterialTheme.typography.bodySmall)
            } }
        } }
    }
}
