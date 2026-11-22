@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.diagnostics

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DiagnosticsContent(
    viewModel: RuntimeDiagnosticsViewModel,
    onOverlay: () -> Unit,
    onListener: () -> Unit,
    onLocation: () -> Unit,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var launchError by remember { mutableStateOf<String?>(null) }
    var expanded by rememberSaveable {
        mutableStateOf(listOf(DiagnosticGroup.CONNECTION.name, DiagnosticGroup.PERMISSIONS.name))
    }
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun action(value: DiagnosticAction) {
        launchError = null
        runCatching {
            when (value) {
                DiagnosticAction.SHEVERY -> viewModel.authorize()
                DiagnosticAction.LISTENER -> onListener()
                DiagnosticAction.OVERLAY -> onOverlay()
                DiagnosticAction.LOCATION -> onLocation()
                DiagnosticAction.NOTIFICATIONS -> context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                DiagnosticAction.LOCATION_SETTINGS -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                DiagnosticAction.BATTERY -> context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                DiagnosticAction.APP_SETTINGS -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")))
            }
        }.onFailure { launchError = "无法打开该设置，请从系统设置中手动查找 OfferPilot" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiagnosticOverview(rows)
        launchError?.let { error ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()) {
                Text(error, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        DiagnosticGroup.entries.forEach { group ->
            val groupRows = rows.filter { it.group == group }
                .sortedBy { if (it.tone == DiagnosticTone.ATTENTION) 0 else 1 }
            val open = group.name in expanded
            DiagnosticSectionHeader(group, groupRows, open) {
                expanded = if (open) expanded - group.name else expanded + group.name
            }
            if (open) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupRows.forEach { row -> DiagnosticStatusCard(row, ::action) }
                }
            }
        }
    }
}
