package io.github.pennymorant.offerpilot.ui.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.ui.components.ToggleRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun NodeDiagnosticDialog(viewModel: NodeDiagnosticViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        viewModel.open()
        onDispose { viewModel.close() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.systemBarsPadding().padding(12.dp).widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight(0.94f),
            shape = MaterialTheme.shapes.extraLarge) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    NodeDiagnosticHeader(state, viewModel::captureAfterDelay, onDismiss)
                }
                    state.pageSummary?.let { item { Text(it) } }
                    if (state.nodes.isNotEmpty()) {
                        item { Text("节点示意图（非截图）：灰框为可见节点，主题辅色粗框为 OCR 区域，主色框为选中节点") }
                        item { NodeRegionDiagram(state) }
                        item { Text("报价容器：${state.selectedRoot.takeIf { it >= 0 } ?: "整页回退"}；" +
                            "OCR：${if (state.ocrBounds != null) "局部区域候选" else "未定位，整屏回退候选"}") }
                        item { Text("动作标签候选：${state.actionCandidates.joinToString { "#$it" }.ifEmpty { "未找到" }}。" +
                            "候选不代表可以执行，还需唯一按钮、父节点及完整订单复核。") }
                        item { ToggleRow("显示节点文字", "可能包含商家或地址，仅本次查看", state.showText,
                            onChange = viewModel::showText) }
                    }
                    items(state.nodes, key = { it.index }) { node ->
                        Surface(Modifier.fillMaxWidth().clickable { viewModel.select(node.index) },
                            color = if (node.index == state.selectedNode) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.small) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("#${node.index} ← ${node.parentIndex} · 可见 ${node.visible} · 启用 ${node.enabled} · 点击 ${node.clickable}")
                                Text(node.viewId ?: "无资源 ID", style = MaterialTheme.typography.bodySmall)
                                Text("${node.bounds.left},${node.bounds.top} — ${node.bounds.right},${node.bounds.bottom}",
                                    style = MaterialTheme.typography.bodySmall)
                                if (state.showText) SelectionContainer {
                                    Text("文字：${node.text.orEmpty()}\n描述：${node.description.orEmpty()}",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NodeDiagnosticHeader(state: NodeDiagnosticState, onCapture: () -> Unit, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("临时节点诊断", style = MaterialTheme.typography.titleLarge)
        Text("诊断期间不自动接单、不将抓取内容计入统计。关闭清空；已有平台订单不会取消。",
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = onCapture, enabled = !state.busy, shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("5 秒后抓取") }
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("关闭并清空") }
        Text(if (state.countdown > 0) "${state.countdown} 秒后抓取 · ${state.message}" else state.message)
    }
}
