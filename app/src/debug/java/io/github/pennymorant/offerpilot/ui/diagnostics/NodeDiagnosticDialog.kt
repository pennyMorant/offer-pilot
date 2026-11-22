@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.github.pennymorant.offerpilot.ui.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.ui.components.ToggleRow
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import io.github.pennymorant.offerpilot.ui.components.groupedItemShape

@Composable
fun NodeDiagnosticDialog(viewModel: NodeDiagnosticViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    DisposableEffect(viewModel) {
        viewModel.open()
        onDispose { viewModel.close() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("临时节点诊断", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    FilledTonalIconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭并清空", modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("诊断期间不自动接单、不计入统计；关闭后自动清空。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = viewModel::captureAfterDelay, enabled = !state.busy,
                            shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.countdown > 0) "${state.countdown} 秒后抓取…" else "5 秒后抓取当前窗口",
                                style = MaterialTheme.typography.titleMedium)
                        }
                        ToolBadge(text = state.message)
                    }
                }
            }
            state.pageSummary?.let { summary ->
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(summary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
                    }
                }
            }
            if (state.nodes.isNotEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("节点布局示意（非截图）", style = MaterialTheme.typography.titleMedium)
                            NodeRegionDiagram(state)
                            Text("容器：${state.selectedRoot.takeIf { it >= 0 } ?: "整页"} · " +
                                "OCR：${if (state.ocrBounds != null) "局部候选" else "整屏回退"} · " +
                                "动作候选：${state.actionCandidates.joinToString { "#$it" }.ifEmpty { "无" }}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ToggleRow("显示节点文字", "仅本次诊断查看，关闭即清空", state.showText, onChange = viewModel::showText)
                        }
                    }
                }
                itemsIndexed(state.nodes, key = { _, n -> n.index }) { idx, node ->
                    val selected = node.index == state.selectedNode
                    Surface(
                        onClick = { viewModel.select(node.index) },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = groupedItemShape(idx, state.nodes.size),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("#${node.index} ← ${node.parentIndex} · 可见=${node.visible} · 点击=${node.clickable}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"))
                            Text("${node.viewId ?: "无资源 ID"} · [${node.bounds.left},${node.bounds.top}–${node.bounds.right},${node.bounds.bottom}]",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.showText) SelectionContainer {
                                Text("文字：${node.text.orEmpty()} | 描述：${node.description.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
