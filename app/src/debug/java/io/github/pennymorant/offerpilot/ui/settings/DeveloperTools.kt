package io.github.pennymorant.offerpilot.ui.settings

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
import io.github.pennymorant.offerpilot.OverudApp
import io.github.pennymorant.offerpilot.di.OverudViewModelFactory
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.overlay.OfferOverlay
import io.github.pennymorant.offerpilot.ui.diagnostics.NodeDiagnosticDialog
import io.github.pennymorant.offerpilot.ui.imagetest.ImageTestDialog
import io.github.pennymorant.offerpilot.ui.rules.RulePreviewDialog
import io.github.pennymorant.offerpilot.work.assessOffer

internal const val hasDeveloperTools = true

private var debugOverlay: OfferOverlay? = null

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperTools(factory: OverudViewModelFactory) {
    var selected by remember { mutableStateOf<String?>(null) }
    var simStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as OverudApp).container }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeveloperToolLinks { selected = it }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("弹窗与通知联调", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return@FilledTonalButton
                    }
                    if (!container.state.value.working && debugOverlay == null) {
                        debugOverlay = OfferOverlay(context, container).also { it.show() }
                        container.registerDebugOverlayCleanup { debugOverlay?.close(); debugOverlay = null }
                    }
                    val now = System.currentTimeMillis()
                    val demo = when (simStep % 4) {
                        0 -> demoExclusiveOffer(now)
                        1 -> demoRadarOffer(now)
                        2 -> demoFakePreferredOffer(now)
                        else -> demoDoorDashShopOffer(now)
                    }
                    simStep++
                    container.session.update {
                        it.copy(latest = demo, decision = it.assessOffer(demo, now), message = "测试模式：模拟订单检测弹窗")
                    }
                }) { Text("模拟订单弹窗") }
                OutlinedButton(shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                    debugOverlay?.close()
                    debugOverlay = null
                    container.session.update { it.copy(latest = null, decision = null, message = "已清除模拟弹窗") }
                }) { Text("清除模拟") }
            }
        }
    }
    when (selected) {
        "image" -> ImageTestDialog(viewModel(factory = factory)) { selected = null }
        "nodes" -> NodeDiagnosticDialog(viewModel(factory = factory)) { selected = null }
        "rules" -> RulePreviewDialog(viewModel(factory = factory)) { selected = null }
    }
}

private fun demoExclusiveOffer(now: Long) = Offer(
    id = "demo-exclusive-$now",
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
    channel = OfferChannel.EXCLUSIVE,
)

private fun demoRadarOffer(now: Long) = Offer(
    id = "demo-radar-$now",
    platform = Platform.UBER_EATS,
    observedAt = now,
    amountCents = 1150,
    distanceMeters = 4989, // ~3.1 mi
    durationSeconds = 1080, // 18 min
    merchant = "Sweetfin (Pasadena)",
    destination = "Lake Ave, Pasadena",
    reliable = true,
    evidence = QuoteEvidence(amountVerified = true, totalsVerified = true, source = OfferSource.UI_NODES,
        distanceVerified = true, durationVerified = true),
    serviceKind = OfferService.DELIVERY,
    includesTip = true,
    channel = OfferChannel.TRIP_RADAR,
)

private fun demoDoorDashShopOffer(now: Long) = Offer(
    id = "demo-dd-shop-$now",
    platform = Platform.DOORDASH,
    observedAt = now,
    amountCents = 2450,
    distanceMeters = 6759, // ~4.2 mi
    durationSeconds = 2280, // 38 min
    merchant = "Sprouts Farmers Market",
    destination = "Beverly Blvd, Los Angeles",
    reliable = true,
    evidence = QuoteEvidence(amountVerified = true, totalsVerified = true, source = OfferSource.UI_NODES,
        distanceVerified = true, durationVerified = true),
    serviceKind = OfferService.SHOP_AND_PAY,
    includesTip = true,
    channel = OfferChannel.EXCLUSIVE,
    itemCount = 12,
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
