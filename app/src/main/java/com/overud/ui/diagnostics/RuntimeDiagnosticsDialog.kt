package com.overud.ui.diagnostics

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun RuntimeDiagnosticsDialog(
    viewModel: RuntimeDiagnosticsViewModel,
    onOverlay: () -> Unit,
    onListener: () -> Unit,
    onLocation: () -> Unit,
    onDismiss: () -> Unit,
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
        }.onFailure {
            launchError = "无法打开该设置，请从系统设置中手动查找 OfferPilot"
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.systemBarsPadding().padding(12.dp).widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight(0.96f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("连接与权限", style = MaterialTheme.typography.titleLarge)
                        Text("检查连接 · 管理授权", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = viewModel::refresh, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Refresh, "刷新状态")
                    }
                    IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Close, "关闭连接与权限")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item("overview") { DiagnosticOverview(rows) }
                    launchError?.let { error -> item("error") {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large) {
                            Text(error, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    } }
                    DiagnosticGroup.entries.forEach { group ->
                        val groupRows = rows.filter { it.group == group }
                            .sortedBy { if (it.tone == DiagnosticTone.ATTENTION) 0 else 1 }
                        val open = group.name in expanded
                        item("header_${group.name}") {
                            DiagnosticSectionHeader(group, groupRows, open) {
                                expanded = if (open) expanded - group.name else expanded + group.name
                            }
                        }
                        if (open) items(groupRows, key = { it.title }) { row ->
                            DiagnosticStatusCard(row, ::action)
                        }
                    }
                }
            }
        }
    }
}
