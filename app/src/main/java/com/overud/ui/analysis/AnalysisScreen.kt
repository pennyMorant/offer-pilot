package com.overud.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.AnalysisPage
import com.overud.ui.components.AnalysisSelection

private enum class AnalysisMode(val label: String) { REGIONS("区域机会"), TIMES("时段趋势") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(state: UnifiedAnalysisUiState, onSelection: (AnalysisSelection) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(AnalysisMode.REGIONS) }
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            AnalysisMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = mode == item,
                    onClick = { mode = item },
                    shape = SegmentedButtonDefaults.itemShape(index, AnalysisMode.entries.size),
                    icon = {
                        Icon(
                            if (item == AnalysisMode.REGIONS) Icons.Rounded.Map else Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text(item.label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        AnalysisPage(
            state = if (mode == AnalysisMode.REGIONS) state.regions else state.times,
            onSelection = onSelection,
            byRegion = mode == AnalysisMode.REGIONS,
        )
    }
}
