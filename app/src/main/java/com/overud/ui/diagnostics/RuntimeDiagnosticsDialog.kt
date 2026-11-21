package com.overud.ui.diagnostics

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("运行状态与权限", style = MaterialTheme.typography.titleLarge)
                Text("打开页面只读取状态，不开始工作、不自动授权。设置变更后返回会刷新。",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::refresh) { Text("刷新状态") }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                launchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(rows, key = { it.title }) { row ->
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(row.title, style = MaterialTheme.typography.titleSmall)
                                Text(row.status, style = MaterialTheme.typography.bodyLarge)
                                Text(row.detail, style = MaterialTheme.typography.bodySmall)
                                row.action?.let { target -> TextButton(onClick = { action(target) }) {
                                    Text(if (target == DiagnosticAction.SHEVERY) "检查并授权" else "打开对应设置")
                                } }
                            }
                        }
                    }
                }
            }
        }
    }
}
