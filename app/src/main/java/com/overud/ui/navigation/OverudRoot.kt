@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.di.OverudViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overud.ui.dashboard.DashboardViewModel
import com.overud.ui.rules.RulesViewModel
import com.overud.ui.dashboard.DashboardScreen
import com.overud.ui.statistics.StatisticsViewModel
import com.overud.ui.regions.RegionsViewModel
import com.overud.ui.statistics.StatisticsScreen
import com.overud.ui.regions.RegionsScreen
import com.overud.ui.rules.RulesScreen

@Composable fun OverudRoot(factory: OverudViewModelFactory, onWork: () -> Unit, onOverlayPermission: () -> Unit, onNotificationPermission: () -> Unit, onLocationPermission: () -> Unit) {
    val dashboard: DashboardViewModel = viewModel(factory = factory)
    val rules: RulesViewModel = viewModel(factory = factory)
    val voice: com.overud.ui.rules.VoiceViewModel = viewModel(factory = factory)
    val runtimeDiagnostics: com.overud.ui.diagnostics.RuntimeDiagnosticsViewModel = viewModel(factory = factory)
    val state by dashboard.state.collectAsStateWithLifecycle()
    val statisticsViewModel: StatisticsViewModel = viewModel(factory = factory)
    val regions: RegionsViewModel = viewModel(factory = factory)
    val statistics by statisticsViewModel.state.collectAsStateWithLifecycle()
    val regionState by regions.state.collectAsStateWithLifecycle()
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
    androidx.activity.compose.BackHandler(enabled = settings || dirty) {
        navigate { settings = false; selected = Destination.DASHBOARD }
    }
    var showRuntimeDiagnostics by rememberSaveable { mutableStateOf(false) }
    var resolve by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (settings) TextButton(onClick = { navigate { settings = false } }) { Text("返回") }
                Text(if (settings) "设置" else selected.label, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (!settings) TextButton(onClick = { navigate { settings = true } }) { Text("设置 ⚙") }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(selected = selected == destination, onClick = { navigate { selected = destination; settings = false } },
                        icon = { Text(destination.symbol, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(destination.label) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (settings) com.overud.ui.settings.SettingsScreen(factory, rules, voice) { showRuntimeDiagnostics = true }
            else AnimatedContent(selected, label = "navigation") { page ->
                when (page) {
                    Destination.DASHBOARD -> DashboardScreen(state, bridge, onWork, dashboard::setAutomatic, dashboard::authorize,
                        { navigate { selected = Destination.RULES } }, { resolve = true })
                    Destination.STATISTICS -> StatisticsScreen(statistics, statisticsViewModel::select)
                    Destination.REGIONS -> RegionsScreen(regionState, regions::select)
                    Destination.RULES -> RulesScreen(rules)
                }
            }
        }
    }
    if (pendingNavigation != null) AlertDialog(onDismissRequest = { pendingNavigation = null },
        title = { Text("还有未保存的修改") }, text = { Text("继续编辑并保存，或放弃修改后离开。") },
        confirmButton = { TextButton(onClick = { pendingNavigation = null }) { Text("继续编辑") } },
        dismissButton = { TextButton(onClick = {
            if (!rules.state.value.saving) {
                rules.discardChanges()
                val action = pendingNavigation
                pendingNavigation = null
                action?.invoke()
            }
        }) { Text("放弃并离开") } })
    if (showRuntimeDiagnostics) com.overud.ui.diagnostics.RuntimeDiagnosticsDialog(runtimeDiagnostics,
        onOverlayPermission, onNotificationPermission, onLocationPermission) { showRuntimeDiagnostics = false }
    if (resolve) AlertDialog(onDismissRequest = { resolve = false }, title = { Text("核对当前配送状态") },
        text = { Text("请先查看 Uber 和 DoorDash，确认当前没有正在进行的配送，再清除订单占用状态。此操作不会取消平台订单。") },
        confirmButton = { TextButton(onClick = { dashboard.resolvePending(); resolve = false }) { Text("确认当前无配送") } },
        dismissButton = { TextButton(onClick = { resolve = false }) { Text("继续暂停") } })
}
