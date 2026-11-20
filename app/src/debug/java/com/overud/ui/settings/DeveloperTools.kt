package com.overud.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overud.OverudApp
import com.overud.di.OverudViewModelFactory
import com.overud.model.*
import com.overud.overlay.OfferOverlay
import com.overud.ui.diagnostics.NodeDiagnosticDialog
import com.overud.ui.imagetest.ImageTestDialog
import com.overud.ui.rules.RulePreviewDialog
import com.overud.work.assessOffer

private var debugOverlay: OfferOverlay? = null

@Composable
fun DeveloperTools(factory: OverudViewModelFactory) {
    var selected by remember { mutableStateOf<String?>(null) }
    var simStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as OverudApp).container }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("开发工具 · 仅测试版本", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { selected = "image" }) { Text("订单图片识别") }
        TextButton(onClick = { selected = "nodes" }) { Text("临时节点诊断") }
        TextButton(onClick = { selected = "rules" }) { Text("文本规则调试") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                if (!Settings.canDrawOverlays(context)) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return@FilledTonalButton
                }
                if (!container.state.value.working && debugOverlay == null) {
                    debugOverlay = OfferOverlay(context, container).also { it.show() }
                }
                val now = System.currentTimeMillis()
                val demo = if (simStep % 2 == 0) demoRadarOffer(now) else demoFakePreferredOffer(now)
                simStep++
                container.session.update {
                    it.copy(latest = demo, decision = it.assessOffer(demo, now), message = "测试模式：模拟雷达单弹窗")
                }
            }) { Text("模拟雷达单弹窗") }
            OutlinedButton(onClick = {
                debugOverlay?.close()
                debugOverlay = null
                container.session.update { it.copy(latest = null, decision = null, message = "已清除模拟弹窗") }
            }) { Text("清除模拟") }
        }
    }
    when (selected) {
        "image" -> ImageTestDialog(viewModel(factory = factory)) { selected = null }
        "nodes" -> NodeDiagnosticDialog(viewModel(factory = factory)) { selected = null }
        "rules" -> RulePreviewDialog(viewModel(factory = factory)) { selected = null }
    }
}

private fun demoRadarOffer(now: Long) = Offer(
    id = "demo-radar-$now",
    platform = Platform.UBER_EATS,
    observedAt = now,
    amountCents = 1480,
    distanceMeters = 6115, // ~3.8 mi
    durationSeconds = 1320, // 22 min
    merchant = "Din Tai Fung (Arcadia)",
    destination = "Santa Anita Ave, Arcadia",
    reliable = true,
    evidence = QuoteEvidence(amountVerified = true, totalsVerified = true, source = OfferSource.UI_NODES,
        distanceVerified = true, durationVerified = true),
    serviceKind = OfferService.DELIVERY,
    includesTip = true,
    channel = OfferChannel.TRIP_RADAR,
)

private fun demoFakePreferredOffer(now: Long) = Offer(
    id = "demo-fake-$now",
    platform = Platform.UBER_EATS,
    observedAt = now,
    amountCents = 1820,
    distanceMeters = 23335, // ~14.5 mi
    durationSeconds = 2520, // 42 min
    merchant = "Prime Pizza (DTLA)",
    destination = "Woodland Hills, Los Angeles",
    reliable = true,
    evidence = QuoteEvidence(amountVerified = true, totalsVerified = true, source = OfferSource.UI_NODES,
        distanceVerified = true, durationVerified = true),
    serviceKind = OfferService.DELIVERY,
    includesTip = true,
    channel = OfferChannel.EXCLUSIVE,
    preferredTag = true,
)
