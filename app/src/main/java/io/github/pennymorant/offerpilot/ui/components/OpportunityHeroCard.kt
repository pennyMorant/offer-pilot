@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun OpportunityHeroCard(state: OpportunityCardState) {
    var showDetail by remember { mutableStateOf(false) }
    Surface(
        onClick = { showDetail = true },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToolBadge(
                        text = "最佳历史热区",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(state.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${state.proximity} · ${state.confidence}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    state.score?.let { "${it}分" } ?: "—",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStatChip("优质率", state.goodRate?.let { "$it%" } ?: "—", Modifier.weight(1f))
                HeroStatChip("频次", state.offersPerHour?.let { String.format(Locale.getDefault(), "%.1f/h", it) } ?: "—", Modifier.weight(1f))
                HeroStatChip("均每英里", "${state.averageMile}/mi", Modifier.weight(1f))
            }
        }
    }
    if (showDetail) OpportunityDetailSheet(state) { showDetail = false }
}

@Composable
private fun HeroStatChip(label: String, value: String, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
