package com.overud.ui.statistics

import androidx.compose.runtime.Composable
import com.overud.ui.components.*

@Composable fun StatisticsScreen(state: AnalysisUiState, onSelection: (AnalysisSelection) -> Unit) {
    AnalysisPage(state, onSelection, byRegion = false)
}
