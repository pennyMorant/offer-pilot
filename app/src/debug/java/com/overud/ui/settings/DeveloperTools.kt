package com.overud.ui.settings

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overud.di.OverudViewModelFactory
import com.overud.ui.imagetest.*
import com.overud.ui.diagnostics.*
import com.overud.ui.rules.*

@Composable fun DeveloperTools(factory: OverudViewModelFactory) {
    var selected by remember { mutableStateOf<String?>(null) }
    Column {
        Text("开发工具 · 仅测试版本", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { selected = "image" }) { Text("订单图片识别") }
        TextButton(onClick = { selected = "nodes" }) { Text("临时节点诊断") }
        TextButton(onClick = { selected = "rules" }) { Text("文本规则调试") }
    }
    when (selected) {
        "image" -> ImageTestDialog(viewModel(factory = factory)) { selected = null }
        "nodes" -> NodeDiagnosticDialog(viewModel(factory = factory)) { selected = null }
        "rules" -> RulePreviewDialog(viewModel(factory = factory)) { selected = null }
    }
}
