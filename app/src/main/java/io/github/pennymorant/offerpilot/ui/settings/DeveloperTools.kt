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
import io.github.pennymorant.offerpilot.model.Policy
import io.github.pennymorant.offerpilot.overlay.OfferOverlay
import io.github.pennymorant.offerpilot.ui.diagnostics.NodeDiagnosticDialog
import io.github.pennymorant.offerpilot.ui.imagetest.ImageTestDialog
import io.github.pennymorant.offerpilot.ui.rules.RulePreviewDialog
import io.github.pennymorant.offerpilot.valuation.EligibilityEvaluator

internal const val hasDeveloperTools = true

private var debugOverlay: OfferOverlay? = null

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperTools(factory: OverudViewModelFactory) {
    var selected by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as OverudApp).container }

    fun triggerDemo(tier: DemoTier) {
        if (!Settings.canDrawOverlays(context)) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!container.state.value.working && debugOverlay == null) {
            debugOverlay = OfferOverlay(context, container).also { it.show() }
            container.registerDebugOverlayCleanup { debugOverlay?.close(); debugOverlay = null }
        }
        val now = System.currentTimeMillis()
        val demo = buildDemoOffer(tier, now)
        val decision = EligibilityEvaluator.assess(demo, Policy(), now)
        container.session.update {
            it.copy(latest = demo, decision = decision, message = "测试模式：${tier.buttonLabel}")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DeveloperToolLinks { selected = it }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("模拟订单弹窗（按档位）", style = MaterialTheme.typography.titleMedium)
                listOf(DemoTier.EXCELLENT to DemoTier.GOOD, DemoTier.BORDERLINE to DemoTier.SKIP).forEach { (left, right) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            onClick = { triggerDemo(left) }) { Text(left.buttonLabel) }
                        FilledTonalButton(shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            onClick = { triggerDemo(right) }) { Text(right.buttonLabel) }
                    }
                }
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
