package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.Valuation

@Composable
fun ValueRatingSummary(valuation: Valuation?) {
    val display = valueRatingPresentation(valuation)
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("价值分", style = MaterialTheme.typography.labelMedium)
                    Text(display.score, style = MaterialTheme.typography.headlineMedium)
                }
                Column {
                    Text(display.grade, style = MaterialTheme.typography.titleMedium)
                    Text(display.confidence, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text("扣车辆成本后 ${display.hourly}/h · ${display.perMile}/mi")
            Text(display.timeBasis, style = MaterialTheme.typography.bodySmall)
        }
    }
}
