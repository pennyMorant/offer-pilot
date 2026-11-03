package com.overud.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.AnalysisPage
import com.overud.ui.components.AnalysisSelection

private enum class AnalysisMode { REGIONS, TIMES }

@Composable
fun AnalysisScreen(state: UnifiedAnalysisUiState, onSelection: (AnalysisSelection) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(AnalysisMode.REGIONS) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(mode == AnalysisMode.REGIONS, onClick = { mode = AnalysisMode.REGIONS },
                label = { Text("区域机会") })
            FilterChip(mode == AnalysisMode.TIMES, onClick = { mode = AnalysisMode.TIMES },
                label = { Text("时段趋势") })
        }
        AnalysisPage(
            state = if (mode == AnalysisMode.REGIONS) state.regions else state.times,
            onSelection = onSelection,
            byRegion = mode == AnalysisMode.REGIONS,
        )
    }
}
