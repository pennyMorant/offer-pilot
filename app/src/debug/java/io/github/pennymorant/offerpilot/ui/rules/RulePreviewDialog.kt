@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.Prop22Summary
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import io.github.pennymorant.offerpilot.ui.components.VehicleCostSummary

@Composable
fun RulePreviewDialog(viewModel: RulePreviewViewModel, onDismiss: () -> Unit) {
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("文本规则调试", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                FilledTonalIconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", modifier = Modifier.size(20.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                Platform.entries.forEachIndexed { idx, item ->
                    val shapes = if (idx == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ToggleButton(
                        checked = preview.platform == item,
                        onCheckedChange = { viewModel.updatePreview(platform = item) },
                        shapes = shapes, modifier = Modifier.weight(1f),
                    ) { Text(item.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = preview.text,
                        onValueChange = { viewModel.updatePreview(text = it) },
                        label = { Text("输入模拟报价文本") },
                        shape = MaterialTheme.shapes.large,
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("仅按已保存规则评估，不接单、不入库、不查目的地区域。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("评估结果", style = MaterialTheme.typography.titleMedium)
                    preview.pageSummary?.let { ToolBadge(text = it) }
                    Text(preview.explanation, style = MaterialTheme.typography.bodyMedium)
                    preview.valuation?.let { VehicleCostSummary(it) }
                    Prop22Summary(preview.valuation?.prop22)
                }
            }
        }
    }
}
