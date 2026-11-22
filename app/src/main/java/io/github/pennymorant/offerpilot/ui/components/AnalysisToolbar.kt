@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AnalysisToolbar(
    byRegion: Boolean,
    delivery: Boolean,
    hasCustomFilter: Boolean,
    filterSummary: String,
    onDelivery: (Boolean) -> Unit,
    onFilter: () -> Unit,
    onInfo: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            ToggleButton(
                checked = !delivery, onCheckedChange = { onDelivery(false) },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) { Text(if (byRegion) "机会" else "质量", style = MaterialTheme.typography.labelLarge) }
            ToggleButton(
                checked = delivery, onCheckedChange = { onDelivery(true) },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) { Text("配送", style = MaterialTheme.typography.labelLarge) }
        }
        Spacer(Modifier.weight(1f))
        FilledTonalButton(
            onClick = onFilter,
            shapes = ButtonDefaults.shapes(),
            colors = if (hasCustomFilter) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) else ButtonDefaults.filledTonalButtonColors(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(filterSummary, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        IconButton(onClick = onInfo, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Info, contentDescription = "统计口径", modifier = Modifier.size(20.dp))
        }
    }
}
