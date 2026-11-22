@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToolBadge

@Composable
internal fun OverudTopBar(
    settings: Boolean,
    inRulesSubPage: Boolean,
    title: String,
    scopeLabel: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            if (settings || inRulesSubPage) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            }
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = {
            if (inRulesSubPage) {
                Box(Modifier.padding(end = 12.dp)) { ToolBadge(text = scopeLabel) }
            } else if (!settings) {
                FilledTonalIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(end = 8.dp),
                ) { Icon(Icons.Rounded.Settings, contentDescription = "设置", modifier = Modifier.size(20.dp)) }
            }
        },
    )
}
