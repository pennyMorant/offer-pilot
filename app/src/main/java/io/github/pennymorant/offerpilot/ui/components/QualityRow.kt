@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QualityRow(state: QualityCardState, index: Int = 0, count: Int = 1) {
    var detail by remember { mutableStateOf(false) }
    Surface(
        onClick = { detail = true },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = groupedItemShape(index, count),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(state.subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ToolBadge(
                    text = state.goodRate?.let { "优质 $it%" } ?: "样本不足",
                    containerColor = if ((state.goodRate ?: 0) >= 55) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if ((state.goodRate ?: 0) >= 55) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.total > 0) {
                Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        state.good to MaterialTheme.colorScheme.primary,
                        state.normal to MaterialTheme.colorScheme.secondary,
                        (state.low + state.unknown) to MaterialTheme.colorScheme.outlineVariant,
                    ).filter { it.first > 0 }.forEach { (n, color) ->
                        Box(Modifier.weight(n.toFloat() / state.total).fillMaxHeight().background(color))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.averageMile}/mi · ${state.total} 张",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"))
                Text("优 ${state.good} · 普通 ${state.normal} · 低 ${state.low}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (detail) {
        ModalBottomSheet(onDismissRequest = { detail = false }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                QualityCard(state)
            }
        }
    }
}
