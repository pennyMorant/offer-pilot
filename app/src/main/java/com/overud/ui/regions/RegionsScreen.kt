package com.overud.ui.regions

import androidx.compose.runtime.Composable
import com.overud.ui.components.*

@Composable fun RegionsScreen(state: AnalysisUiState, onSelection: (AnalysisSelection) -> Unit) {
    AnalysisPage(state, onSelection, byRegion = true)
}
