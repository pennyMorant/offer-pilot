@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import com.overud.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable fun OrdersScreen(records: List<OrderSummary>) {
    val locale = LocalConfiguration.current.locales[0]
    var detail by remember { mutableStateOf<OrderSummary?>(null) }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("YOUR ORDER JOURNAL", "每一单，有迹可循。", "记录收到的报价，区分报价与实际收入。") }
        if (records.isEmpty()) item { EmptyStateCard("还没有订单记录", "订单识别后会保存在本机，未接的报价也会参与价值统计。") }
        items(records, key = { it.id }) { record ->
            Card(onClick = { detail = record }) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(record.platformLabel, style = MaterialTheme.typography.titleMedium)
                        Text(SimpleDateFormat("MM/dd HH:mm", locale).format(Date(record.observedAt)), style = MaterialTheme.typography.labelMedium)
                        Text(if (record.eligible) "符合当时规则" else "未满足接单要求", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(record.amountCents?.let(::money) ?: "—", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
    detail?.let { record -> ModalBottomSheet(onDismissRequest = { detail = null }) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("报价分析", style = MaterialTheme.typography.headlineLarge)
            Text(record.amountCents?.let(::money) ?: "金额未知", style = MaterialTheme.typography.displayLarge)
            Text(record.explanation)
            Text("目的地：${record.destination ?: "未识别"}")
            Text("规则版本 ${record.policyVersion} · 此金额为报价，尚非结算收入", style = MaterialTheme.typography.bodySmall)
        }
    } }
}
