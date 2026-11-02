package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.overud.model.DestinationAssessment

@Composable
fun DestinationSummary(assessment: DestinationAssessment) {
    val display = destinationPresentation(assessment, LocalConfiguration.current.locales[0])
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(display.title, style = MaterialTheme.typography.titleMedium)
            Text(display.detail, style = MaterialTheme.typography.bodySmall)
            display.timing?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            display.evidence?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("后续接单等待与概率尚无可靠统计；历史质量仅供参考。", style = MaterialTheme.typography.bodySmall)
        }
    }
}
