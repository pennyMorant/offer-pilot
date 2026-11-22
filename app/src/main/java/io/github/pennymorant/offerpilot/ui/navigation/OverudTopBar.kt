@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToolBadge

@Composable
internal fun OverudTopBar(
    inSubPage: Boolean,
    title: String,
    badgeText: String? = null,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            if (inSubPage) {
                IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            }
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = {
            badgeText?.let {
                Box(Modifier.padding(end = 12.dp)) { ToolBadge(text = it) }
            }
            onRefresh?.let { refresh ->
                FilledTonalIconButton(
                    onClick = refresh,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.padding(end = 8.dp),
                ) { Icon(Icons.Rounded.Refresh, contentDescription = "刷新状态", modifier = Modifier.size(20.dp)) }
            }
        },
    )
}

@Composable
internal fun OverudDialogs(
    hasPendingNav: Boolean,
    savingRules: Boolean,
    showResolve: Boolean,
    onDismissNav: () -> Unit,
    onDiscardAndNav: () -> Unit,
    onDismissResolve: () -> Unit,
    onConfirmResolve: () -> Unit,
) {
    if (hasPendingNav) AlertDialog(
        onDismissRequest = onDismissNav,
        title = { Text("还有未保存的修改") },
        text = { Text("继续编辑并保存，或放弃修改后离开。") },
        confirmButton = { TextButton(onClick = onDismissNav) { Text("继续编辑") } },
        dismissButton = {
            TextButton(onClick = { if (!savingRules) onDiscardAndNav() }) { Text("放弃并离开") }
        },
    )
    if (showResolve) AlertDialog(
        onDismissRequest = onDismissResolve,
        title = { Text("确认当前订单状态") },
        text = { Text("只在你已确认平台上没有进行中的配送或接单时继续。") },
        confirmButton = { TextButton(onClick = onConfirmResolve) { Text("已确认，恢复监听") } },
        dismissButton = { TextButton(onClick = onDismissResolve) { Text("取消") } },
    )
}
