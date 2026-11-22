package io.github.pennymorant.offerpilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DASHBOARD("工作台", Icons.Rounded.Speed, Icons.Outlined.Speed),
    ANALYSIS("分析", Icons.Rounded.Analytics, Icons.Outlined.Analytics),
    RULES("规则", Icons.Rounded.Tune, Icons.Outlined.Tune),
    SETTINGS("设置", Icons.Rounded.Settings, Icons.Outlined.Settings),
}
