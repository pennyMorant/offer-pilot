@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.overud.domain.*

@Composable fun RulePreviewDialog(policy: Policy, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("Accept\n$12.00\nTotal 5 mi 25 min\nMerchant: Restaurant\nDestination: Downtown") }
    var platform by remember { mutableStateOf(Platform.UBER_EATS) }
    val offer = remember(text, platform) { OfferParser.parse(platform, text, System.currentTimeMillis()) }
    val decision = offer?.let { ValueEngine.assess(it, policy, it.observedAt) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("规则预览") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("仅分析输入文本，不会接单，也不会写入真实订单记录。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Platform.entries.forEach { item -> FilterChip(selected = platform == item, onClick = { platform = item }, label = { Text(item.label) }) } }
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("报价文本") }, minLines = 4, maxLines = 8)
            Text(decision?.reasons?.joinToString("；")?.ifEmpty { "符合当前全部接单要求" } ?: "无法识别报价，请包含接单按钮文字和金额")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}
