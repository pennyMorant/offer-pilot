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
import com.overud.ui.rules.RulePreviewDialog

@Composable fun OverudRoot(factory: OverudViewModelFactory, onWork: () -> Unit, onOverlayPermission: () -> Unit, onNotificationPermission: () -> Unit, onLocationPermission: () -> Unit) {
    val dashboard: DashboardViewModel = viewModel(factory = factory)
    val rules: RulesViewModel = viewModel(factory = factory)
    val state by dashboard.state.collectAsStateWithLifecycle()
    val statisticsViewModel: StatisticsViewModel = viewModel(factory = factory)
    val regions: RegionsViewModel = viewModel(factory = factory)
    val statistics by statisticsViewModel.state.collectAsStateWithLifecycle()
    val regionState by regions.state.collectAsStateWithLifecycle()
    val bridge = state.connection
    var selected by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }
    var replay by rememberSaveable { mutableStateOf(false) }
    var resolve by remember { mutableStateOf(false) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(selected = selected == destination, onClick = { selected = destination },
                        icon = { Text(destination.symbol, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(destination.label) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(selected, label = "navigation") { page ->
                when (page) {
                    Destination.DASHBOARD -> DashboardScreen(state, bridge, onWork, dashboard::setAutomatic, dashboard::authorize,
                        { selected = Destination.RULES }, { replay = true }, { resolve = true })
                    Destination.STATISTICS -> StatisticsScreen(statistics, statisticsViewModel::select)
                    Destination.REGIONS -> RegionsScreen(regionState, regions::select)
                    Destination.RULES -> RulesScreen(rules, onOverlayPermission, onNotificationPermission, onLocationPermission)
                }
            }
        }
    }
    if (replay) RulePreviewDialog(rules) { replay = false }
    if (resolve) AlertDialog(onDismissRequest = { resolve = false }, title = { Text("核对当前配送状态") },
        text = { Text("请先查看 Uber 和 DoorDash，确认当前没有正在进行的配送，再清除订单占用状态。此操作不会取消平台订单。") },
        confirmButton = { TextButton(onClick = { dashboard.resolvePending(); resolve = false }) { Text("确认当前无配送") } },
        dismissButton = { TextButton(onClick = { resolve = false }) { Text("继续暂停") } })
}
