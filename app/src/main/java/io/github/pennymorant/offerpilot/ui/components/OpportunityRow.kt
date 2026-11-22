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
fun OpportunityRow(
    state: OpportunityCardState,
    index: Int = 0,
    count: Int = 1,
    muted: Boolean = false,
) {
    var detail by remember { mutableStateOf(false) }
    val score = state.score
    val ringColor = when {
        score == null -> MaterialTheme.colorScheme.outlineVariant
        score >= 70 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        onClick = { detail = true },
        color = if (muted) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = groupedItemShape(index, count),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (score ?: 0) / 100f },
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeWidth = 4.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    score?.toString() ?: "—",
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(state.proximity, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
                Text(
                    listOfNotNull(
                        state.goodRate?.let { "优质 $it%" },
                        state.offersPerHour?.let { String.format(Locale.getDefault(), "%.1f单/h", it) },
                        "${state.averageMile}/mi",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (detail) OpportunityDetailSheet(state) { detail = false }
}
