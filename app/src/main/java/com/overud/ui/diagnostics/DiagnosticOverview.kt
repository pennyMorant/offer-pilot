package com.overud.ui.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DiagnosticOverview(rows: List<RuntimeDiagnosticRow>) {
    val connection = rows.filter { it.group == DiagnosticGroup.CONNECTION }
    val missing = rows.count { it.group == DiagnosticGroup.PERMISSIONS && it.tone == DiagnosticTone.ATTENTION }
    val unknown = rows.any { it.group == DiagnosticGroup.PERMISSIONS && it.tone == DiagnosticTone.UNKNOWN }
    val needsConnection = connection.any { it.tone == DiagnosticTone.ATTENTION }
    val ready = connection.all { it.tone == DiagnosticTone.READY }
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Hub, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            Text(when {
                needsConnection -> "连接需要处理"
                ready -> "页面读取已就绪"
                else -> "Shevery 已授权"
            }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(when {
                missing > 0 -> "$missing 项权限或系统开关待开启，可在下方逐项处理。"
                unknown -> "正在检查应用权限，暂时无法读取的项目可手动刷新。"
                else -> "应用权限已开启，可返回主页查看工作状态。"
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("从系统设置返回会自动刷新", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun DiagnosticSectionHeader(group: DiagnosticGroup, rows: List<RuntimeDiagnosticRow>, expanded: Boolean, onToggle: () -> Unit) {
    val attention = rows.count { it.tone == DiagnosticTone.ATTENTION }
    Surface(onClick = onToggle, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(group.title, style = MaterialTheme.typography.titleMedium)
                Text(group.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (attention > 0) Badge { Text("$attention 待处理") }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起${group.title}" else "展开${group.title}")
        }
    }
}
