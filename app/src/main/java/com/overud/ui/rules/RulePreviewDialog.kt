package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.model.Platform
import com.overud.ui.components.VehicleCostSummary

@Composable fun RulePreviewDialog(viewModel: RulesViewModel, onDismiss: () -> Unit) {
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("规则预览") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("仅分析输入文本，不会接单，也不会写入真实订单记录。")
            Text("按所选平台的已保存要求预览；未保存的草稿不参与判断。", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach { item ->
                    FilterChip(selected = preview.platform == item,
                        onClick = { viewModel.updatePreview(platform = item) }, label = { Text(item.label) })
                }
            }
            OutlinedTextField(value = preview.text, onValueChange = { viewModel.updatePreview(text = it) },
                label = { Text("报价文本") }, minLines = 4, maxLines = 8)
            Text(preview.explanation)
            preview.valuation?.let { VehicleCostSummary(it) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}
