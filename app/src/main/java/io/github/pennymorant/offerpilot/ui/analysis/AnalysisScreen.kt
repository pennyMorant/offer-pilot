@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.analysis

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.ui.components.AnalysisPage
import io.github.pennymorant.offerpilot.ui.components.AnalysisSelection

private enum class AnalysisMode(val label: String) {
    DAILY("每日复盘"),
    REGIONS("区域机会"),
    TIMES("时段趋势"),
}

@Composable
fun AnalysisScreen(
    state: UnifiedAnalysisUiState,
    dailyState: DailyReviewUiState,
    onSelection: (AnalysisSelection) -> Unit,
    onDailyRange: (DailyDateRange) -> Unit,
    onDailyPlatform: (Platform?) -> Unit,
    onDailyFilter: (DailyItemFilter) -> Unit,
    onClearHistory: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AnalysisMode.DAILY) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            AnalysisMode.entries.forEachIndexed { index, item ->
                val shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    AnalysisMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                ToggleButton(
                    checked = mode == item,
                    onCheckedChange = { mode = item },
                    shapes = shapes,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(
                        when (item) {
                            AnalysisMode.DAILY -> Icons.Rounded.Assessment
                            AnalysisMode.REGIONS -> Icons.Rounded.Map
                            AnalysisMode.TIMES -> Icons.Rounded.Schedule
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(item.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
        val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
            },
            label = "analysisMode",
            modifier = Modifier.weight(1f),
        ) { current ->
            when (current) {
                AnalysisMode.DAILY -> DailyReviewPage(dailyState, onDailyRange, onDailyPlatform, onDailyFilter, onClearHistory)
                AnalysisMode.REGIONS -> AnalysisPage(state.regions, onSelection, byRegion = true)
                AnalysisMode.TIMES -> AnalysisPage(state.times, onSelection, byRegion = false)
            }
        }
    }
}
