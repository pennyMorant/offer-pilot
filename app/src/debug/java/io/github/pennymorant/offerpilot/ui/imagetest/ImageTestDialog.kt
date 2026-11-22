@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.github.pennymorant.offerpilot.ui.imagetest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.ui.components.*

private enum class TestDisplayMode(val label: String) { WORK("分析卡片"), DEBUG("OCR 原文") }

@Composable
fun ImageTestDialog(viewModel: ImageTestViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(TestDisplayMode.WORK) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.selectImage(uri)
    }
    val close = { viewModel.clear(); onDismiss() }

    ModalBottomSheet(
        onDismissRequest = close,
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
                Text("订单图片识别", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                FilledTonalIconButton(onClick = close, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", modifier = Modifier.size(20.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                Platform.entries.forEachIndexed { idx, platform ->
                    val shapes = if (idx == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ToggleButton(
                        checked = state.platform == platform,
                        onCheckedChange = { viewModel.selectPlatform(platform) },
                        shapes = shapes, modifier = Modifier.weight(1f),
                    ) { Text(platform.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                }
            }
            Button(
                enabled = !state.busy, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Icon(Icons.Rounded.ImageSearch, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.hasImage) "重新选择截图" else "选择订单截图", style = MaterialTheme.typography.titleMedium)
            }
            if (state.busy) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LoadingIndicator()
                    Text("正在识别图片…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.error?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp))
                }
            }
            if (state.hasImage) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    TestDisplayMode.entries.forEachIndexed { idx, item ->
                        val shapes = if (idx == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                            else ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ToggleButton(
                            checked = mode == item, onCheckedChange = { mode = item },
                            shapes = shapes, modifier = Modifier.weight(1f),
                        ) { Text(item.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                    }
                }
                when (mode) {
                    TestDisplayMode.WORK -> {
                        state.offer?.let { offer ->
                            OfferCard(offer, state.decision, DestinationAssessment(), customPreferredZones = state.preferredZones)
                        } ?: Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("未解析出报价，可切到「OCR 原文」查看识别结果。", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                        }
                    }
                    TestDisplayMode.DEBUG -> {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                platformPagePresentation(state.page)?.let { ToolBadge(text = it) }
                                state.offer?.let { offer ->
                                    Text("${offer.serviceKind.label} · ${offer.channel.label} · 优选=${offer.preferredTag}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("商家：${offer.merchant ?: "—"} · 目的：${offer.destination ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    state.decision?.reasons?.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                }
                                if (state.text.isNotBlank()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Text("OCR 原始文字（可长按复制）", style = MaterialTheme.typography.labelLarge)
                                    SelectionContainer { Text(state.text, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
