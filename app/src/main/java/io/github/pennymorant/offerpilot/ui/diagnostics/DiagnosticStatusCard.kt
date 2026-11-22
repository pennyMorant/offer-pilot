package io.github.pennymorant.offerpilot.ui.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiagnosticStatusCard(row: RuntimeDiagnosticRow, onAction: (DiagnosticAction) -> Unit) {
    val attention = row.tone == DiagnosticTone.ATTENTION
    var expanded by rememberSaveable(row.title, row.tone) { mutableStateOf(attention) }
    val color = when (row.tone) {
        DiagnosticTone.READY -> MaterialTheme.colorScheme.primary
        DiagnosticTone.ATTENTION -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(when (row.tone) {
                    DiagnosticTone.READY -> Icons.Rounded.CheckCircle
                    DiagnosticTone.ATTENTION -> Icons.Rounded.ErrorOutline
                    DiagnosticTone.INFO -> Icons.Rounded.Info
                    DiagnosticTone.UNKNOWN -> Icons.Rounded.HelpOutline
                }, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(row.title, style = MaterialTheme.typography.titleSmall)
                    Text(row.status, style = MaterialTheme.typography.bodyMedium, color = color)
                }
                IconButton(onClick = { expanded = !expanded }, shapes = IconButtonDefaults.shapes()) {
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "收起${row.title}说明" else "展开${row.title}说明")
                }
            }
            AnimatedVisibility(expanded,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())) {
                Text(row.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            row.action?.let { action ->
                val label = if (action == DiagnosticAction.SHEVERY) "检查并授权"
                    else if (attention) "去开启" else "管理设置"
                if (attention) FilledTonalButton(
                    onClick = { onAction(action) }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                ) {
                    Text(label)
                    Icon(Icons.Rounded.ArrowOutward, null, Modifier.padding(start = 6.dp).size(16.dp))
                } else if (expanded) TextButton(onClick = { onAction(action) }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Text(label)
                }
            }
        }
    }
}
