@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.analytics.DailyOfferSummary
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.ToolBadge
import io.github.pennymorant.offerpilot.ui.components.ToolMetricTile
import io.github.pennymorant.offerpilot.ui.components.money

@Composable
fun TodayShiftCard(
    summary: DailyOfferSummary,
    onOpenPlatform: (Platform) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("今日行情快报", style = MaterialTheme.typography.titleMedium)
                    ToolBadge(
                        text = if (summary.totalCount > 0) "${summary.totalCount} 单 · 好单 ${summary.goodPercent}%"
                            else "暂无推送",
                        containerColor = if (summary.totalCount > 0) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (summary.totalCount > 0) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolMetricTile(
                        label = "均每英里",
                        value = summary.avgCentsPerMile?.let(::money) ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    ToolMetricTile(
                        label = "均毛时薪",
                        value = summary.avgGrossCentsPerHour?.let(::money) ?: "—",
                        modifier = Modifier.weight(1f),
                        highlight = summary.totalCount > 0,
                    )
                    ToolMetricTile(
                        label = "达标率",
                        value = if (summary.totalCount > 0) "${summary.eligiblePercent}%" else "—",
                        modifier = Modifier.weight(0.85f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Platform.entries.forEach { platform ->
                FilledTonalButton(
                    onClick = { onOpenPlatform(platform) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("打开 ${platform.label}", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}
