package com.overud.ui.imagetest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.model.*
import kotlin.math.roundToLong
import com.overud.ui.components.*

@Composable fun ImageTestDialog(viewModel: ImageTestViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.selectImage(uri)
    }
    val close = { viewModel.clear(); onDismiss() }
    AlertDialog(onDismissRequest = close, title = { Text("订单图片测试") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("选择 Uber 或 DoorDash 截图，按已保存规则分析。不接单、不计入统计，关闭后清除本次结果。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach { platform ->
                    FilterChip(selected = state.platform == platform,
                        onClick = { viewModel.selectPlatform(platform) }, label = { Text(platform.label) })
                }
            }
            Button(enabled = !state.busy, onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (state.hasImage) "重新选择图片" else "选择图片") }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("正在识别图片…")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            platformPagePresentation(state.page)?.let { Text(it) }
            state.offer?.let { offer ->
                Text(offerAmountPresentation(offer), style = MaterialTheme.typography.headlineMedium)
                Text(offerRoutePresentation(offer))
                Text("订单类型：${offer.serviceKind.label} · ${offer.channel.label}")
                if (offer.preferredTag) Text("平台标记：优选高价单（Preferred / High Paying）")
                offer.itemCount?.let { Text("商超件数：$it 件") }
                if (offer.serviceKind == OfferService.PACKAGE) Text("取件地址：${offer.pickupAddress ?: "未识别"}")
                else Text("商家：${offer.merchant ?: "未识别"}")
                if (offer.includesTip == true) Text("报价含小费，小费明细未知")
                Text("目的地：${offer.destination ?: "未识别"}")
                Text("每英里：${state.decision?.valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—"}")
                Text("预计毛时薪：${state.decision?.valuation?.grossCentsPerHour?.let { money(it.roundToLong()) } ?: "—"}")
                VehicleCostSummary(state.decision?.valuation)
                Prop22Summary(state.decision?.valuation?.prop22)
                Text("接单判断", style = MaterialTheme.typography.titleSmall)
                state.decision?.reasons?.forEach { Text("• $it") }
                Text("图片来源始终保留手动接单限制；不查询目的地区域，启用区域要求时显示证据不足。",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (state.hasImage && state.text.isNotBlank()) {
                if (state.offer == null) Text("未解析出报价，请检查平台、页面类型及下方文字。")
                Text("OCR 原始文字（可复制）", style = MaterialTheme.typography.titleSmall)
                SelectionContainer { Text(state.text, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }, confirmButton = { TextButton(onClick = close) { Text("关闭") } })
}
