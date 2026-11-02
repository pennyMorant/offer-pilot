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
import com.overud.ui.orders.OrdersScreen
import com.overud.ui.regions.RegionsScreen
import com.overud.ui.rules.RulesScreen
import com.overud.ui.rules.RulePreviewDialog

@Composable fun OverudRoot(factory: OverudViewModelFactory, onWork: () -> Unit, onOverlayPermission: () -> Unit, onNotificationPermission: () -> Unit, onLocationPermission: () -> Unit) {
    val dashboard: DashboardViewModel = viewModel(factory = factory)
    val rules: RulesViewModel = viewModel(factory = factory)
    val state by dashboard.state.collectAsStateWithLifecycle()
    val bridge by dashboard.connection.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var replay by rememberSaveable { mutableStateOf(false) }
    var resolve by remember { mutableStateOf(false) }
    val pages = listOf("工作台", "订单", "区域", "规则")
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                pages.forEachIndexed { index, label ->
                    NavigationBarItem(selected = selected == index, onClick = { selected = index },
                        icon = { Text(listOf("◉", "≡", "⌖", "☷")[index], style = MaterialTheme.typography.titleLarge) },
                        label = { Text(label) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(selected, label = "navigation") { page ->
                when (page) {
                    0 -> DashboardScreen(state, bridge, onWork, dashboard::setAutomatic, dashboard::authorize,
                        { selected = 3 }, { replay = true }, { resolve = true })
                    1 -> OrdersScreen(state.records)
                    2 -> RegionsScreen(state.records, state.regions)
                    else -> RulesScreen(rules, onOverlayPermission, onNotificationPermission, onLocationPermission)
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
