@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun OpportunityDetailSheet(state: OpportunityCardState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(state.subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ToolBadge(text = state.confidence)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    state.score?.let { "${it}分" } ?: "暂未评分",
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
                LinearWavyProgressIndicator(
                    progress = { (state.score ?: 0) / 100f },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolMetricTile(
                        label = "历史优质比例",
                        value = state.goodRate?.let { "$it%" } ?: "样本不足",
                        modifier = Modifier.weight(1f),
                        highlight = (state.goodRate ?: 0) >= 60,
                    )
                    ToolMetricTile(
                        label = "每观察小时",
                        value = state.offersPerHour?.let { String.format(Locale.getDefault(), "%.1f 张", it) } ?: "不足1小时",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolMetricTile("每英里毛报价", "${state.averageMile}/mi", Modifier.weight(1f))
                    ToolMetricTile("每小时毛报价", "${state.averageHour}/h", Modifier.weight(1f))
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.proximity, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(state.advice, style = MaterialTheme.typography.bodyMedium)
                    Text(state.samples, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "指数基于历史观察统计，不是下一单概率；前往成本和实时供需尚未计入。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
