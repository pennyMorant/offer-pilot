package com.overud.ui.imagetest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.model.*
import com.overud.ui.components.*

private enum class TestDisplayMode(val label: String) { WORK("Work 模式"), DEBUG("Debug 模式") }

@OptIn(ExperimentalMaterial3Api::class)
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("订单图片测试", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = close) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach { platform ->
                    FilterChip(selected = state.platform == platform,
                        onClick = { viewModel.selectPlatform(platform) }, label = { Text(platform.label) })
                }
            }
            Button(enabled = !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(46.dp),
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(if (state.hasImage) "重新选择截图" else "选择订单截图", style = MaterialTheme.typography.labelLarge)
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("正在识别图片…", style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (state.hasImage) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TestDisplayMode.entries.forEachIndexed { idx, item ->
                        SegmentedButton(
                            selected = mode == item,
                            onClick = { mode = item },
                            shape = SegmentedButtonDefaults.itemShape(idx, TestDisplayMode.entries.size),
                            icon = {},
                            label = { Text(item.label, style = MaterialTheme.typography.labelLarge) },
                        )
                    }
                }
                when (mode) {
                    TestDisplayMode.WORK -> {
                        state.offer?.let { offer ->
                            OfferCard(
                                offer = offer,
                                decision = state.decision,
                                destination = DestinationAssessment(),
                                customPreferredZones = state.preferredZones,
                            )
                        } ?: Text("未解析出报价，可切到 Debug 模式查看 OCR 原文。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TestDisplayMode.DEBUG -> {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                platformPagePresentation(state.page)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                state.offer?.let { offer ->
                                    Text("类型：${offer.serviceKind.label} · 通道：${offer.channel.label} · 优选：${offer.preferredTag}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("商家：${offer.merchant ?: "—"} · 取件：${offer.pickupAddress ?: "—"} · 目的：${offer.destination ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall)
                                    state.decision?.reasons?.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                }
                                if (state.text.isNotBlank()) {
                                    HorizontalDivider()
                                    Text("OCR 原始文字（可复制）", style = MaterialTheme.typography.titleSmall)
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
