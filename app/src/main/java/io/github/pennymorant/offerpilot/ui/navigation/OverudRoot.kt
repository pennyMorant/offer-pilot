@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pennymorant.offerpilot.di.OverudViewModelFactory
import io.github.pennymorant.offerpilot.ui.analysis.AnalysisScreen
import io.github.pennymorant.offerpilot.ui.analysis.AnalysisViewModel
import io.github.pennymorant.offerpilot.ui.analysis.DailyReviewViewModel
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardScreen
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardViewModel
import io.github.pennymorant.offerpilot.ui.diagnostics.RuntimeDiagnosticsDialog
import io.github.pennymorant.offerpilot.ui.diagnostics.RuntimeDiagnosticsViewModel
import io.github.pennymorant.offerpilot.ui.rules.RulesScreen
import io.github.pennymorant.offerpilot.ui.rules.RulesViewModel
import io.github.pennymorant.offerpilot.ui.rules.VoiceViewModel
import io.github.pennymorant.offerpilot.ui.settings.SettingsScreen

@Composable
fun OverudRoot(
    factory: OverudViewModelFactory,
    onWork: () -> Unit,
    onOverlayPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onLocationPermission: () -> Unit,
) {
    val dashboard: DashboardViewModel = viewModel(factory = factory)
    val rules: RulesViewModel = viewModel(factory = factory)
    val voice: VoiceViewModel = viewModel(factory = factory)
    val runtimeDiagnostics: RuntimeDiagnosticsViewModel = viewModel(factory = factory)
    val analysisViewModel: AnalysisViewModel = viewModel(factory = factory)
    val dailyReviewViewModel: DailyReviewViewModel = viewModel(factory = factory)
    val state by dashboard.state.collectAsStateWithLifecycle()
    val analysis by analysisViewModel.state.collectAsStateWithLifecycle()
    val dailyState by dailyReviewViewModel.state.collectAsStateWithLifecycle()
    val ruleState by rules.state.collectAsStateWithLifecycle()
    val prop22State by rules.prop22.collectAsStateWithLifecycle()
    val dirty = remember(ruleState, prop22State) { rules.hasChanges() }
    val bridge = state.connection
    var selected by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun navigate(action: () -> Unit) {
        if (rules.state.value.saving) return
        if (rules.hasChanges()) pendingNavigation = action else action()
    }
    BackHandler(enabled = settings || dirty) { navigate { settings = false; selected = Destination.DASHBOARD } }
    var showRuntimeDiagnostics by rememberSaveable { mutableStateOf(false) }
    var resolve by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (settings) {
                        IconButton(onClick = { navigate { settings = false } }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                title = { Text(if (settings) "设置" else selected.label, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (!settings) {
                        FilledTonalIconButton(
                            onClick = { navigate { settings = true } },
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Icon(Icons.Rounded.Settings, contentDescription = "设置", modifier = Modifier.size(20.dp)) }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) {
            if (settings) {
                Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                    SettingsScreen(factory, rules, voice) { showRuntimeDiagnostics = true }
                }
            } else {
                AnimatedContent(selected, label = "navigation",
                    modifier = Modifier.fillMaxSize().padding(bottom = 88.dp + padding.calculateBottomPadding())) { page ->
                    when (page) {
                        Destination.DASHBOARD -> DashboardScreen(state, dashboard::setAutomatic,
                            { navigate { selected = Destination.RULES } }, { resolve = true }, dashboard::applyScene,
                            dashboard::adjustUberAr, dashboard::openPlatform)
                        Destination.ANALYSIS -> AnalysisScreen(
                            analysis, dailyState, analysisViewModel::select,
                            dailyReviewViewModel::selectRange, dailyReviewViewModel::selectPlatform,
                            dailyReviewViewModel::selectFilter, dailyReviewViewModel::clearAll,
                        )
                        Destination.RULES -> RulesScreen(rules)
                    }
                }
                BottomNavigationDock(
                    selected = selected,
                    working = state.working,
                    authorized = bridge.authorized,
                    onSelect = { dest -> navigate { selected = dest; settings = false } },
                    onWork = onWork,
                    onAuthorize = dashboard::authorize,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
    if (pendingNavigation != null) AlertDialog(onDismissRequest = { pendingNavigation = null },
        title = { Text("还有未保存的修改") }, text = { Text("继续编辑并保存，或放弃修改后离开。") },
        confirmButton = { TextButton(onClick = { pendingNavigation = null }) { Text("继续编辑") } },
        dismissButton = { TextButton(onClick = {
            if (!rules.state.value.saving) {
                rules.discardChanges(); val action = pendingNavigation; pendingNavigation = null; action?.invoke()
            }
        }) { Text("放弃并离开") } })
    if (showRuntimeDiagnostics) RuntimeDiagnosticsDialog(runtimeDiagnostics,
        onOverlayPermission, onNotificationPermission, onLocationPermission) { showRuntimeDiagnostics = false }
    if (resolve) AlertDialog(onDismissRequest = { resolve = false }, title = { Text("核对当前配送状态") },
        text = { Text("请先查看 Uber 和 DoorDash，确认当前没有正在进行的配送，再清除订单占用状态。此操作不会取消平台订单。") },
        confirmButton = { TextButton(onClick = { dashboard.resolvePending(); resolve = false }) { Text("确认当前无配送") } },
        dismissButton = { TextButton(onClick = { resolve = false }) { Text("继续暂停") } })
}
