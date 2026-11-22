@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pennymorant.offerpilot.di.OverudViewModelFactory
import io.github.pennymorant.offerpilot.ui.analysis.AnalysisScreen
import io.github.pennymorant.offerpilot.ui.analysis.AnalysisViewModel
import io.github.pennymorant.offerpilot.ui.analysis.DailyReviewViewModel
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardScreen
import io.github.pennymorant.offerpilot.ui.dashboard.DashboardViewModel
import io.github.pennymorant.offerpilot.ui.diagnostics.RuntimeDiagnosticsViewModel
import io.github.pennymorant.offerpilot.ui.rules.RulesScreen
import io.github.pennymorant.offerpilot.ui.rules.RulesViewModel
import io.github.pennymorant.offerpilot.ui.rules.VoiceViewModel
import io.github.pennymorant.offerpilot.ui.settings.SettingsScreen

@Composable
fun OverudRoot(
    factory: OverudViewModelFactory,
    onWork: () -> Unit,
    onOverlayPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onLocationPermission: () -> Unit,
) {
    val dashboard: DashboardViewModel = viewModel(factory = factory)
    val rules: RulesViewModel = viewModel(factory = factory)
    val voice: VoiceViewModel = viewModel(factory = factory)
    val diagnostics: RuntimeDiagnosticsViewModel = viewModel(factory = factory)
    val analysisViewModel: AnalysisViewModel = viewModel(factory = factory)
    val dailyReviewViewModel: DailyReviewViewModel = viewModel(factory = factory)
    val state by dashboard.state.collectAsStateWithLifecycle()
    val analysis by analysisViewModel.state.collectAsStateWithLifecycle()
    val dailyState by dailyReviewViewModel.state.collectAsStateWithLifecycle()
    val ruleState by rules.state.collectAsStateWithLifecycle()
    val prop22State by rules.prop22.collectAsStateWithLifecycle()
    val dirty = remember(ruleState, prop22State) { rules.hasChanges() }
    val bridge = state.connection
    var selected by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }
    var rulesSection by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsSection by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }
    var resolve by remember { mutableStateOf(false) }

    fun navigate(action: () -> Unit) {
        if (rules.state.value.saving) return
        if (rules.hasChanges()) pendingNavigation = action else action()
    }
    val inRulesSubPage = selected == Destination.RULES && rulesSection != null
    val inSettingsSubPage = selected == Destination.SETTINGS && settingsSection != null
    val inSubPage = inRulesSubPage || inSettingsSubPage

    BackHandler(enabled = inSubPage || selected != Destination.DASHBOARD || dirty) {
        when {
            inRulesSubPage -> rulesSection = null
            inSettingsSubPage -> settingsSection = null
            else -> navigate { selected = Destination.DASHBOARD }
        }
    }

    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OverudTopBar(
                inSubPage = inSubPage,
                title = when {
                    inRulesSubPage -> rulesSection.orEmpty()
                    inSettingsSubPage -> settingsSection.orEmpty()
                    else -> selected.label
                },
                badgeText = if (inRulesSubPage) (ruleState.scope?.label ?: "公共要求") else null,
                onBack = { if (inRulesSubPage) rulesSection = null else settingsSection = null },
                onRefresh = if (inSettingsSubPage && settingsSection == "连接与权限") diagnostics::refresh else null,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) {
            val bottomInset = if (inSubPage) padding.calculateBottomPadding()
                else 88.dp + padding.calculateBottomPadding()
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    (slideInHorizontally(spatialSpec) { if (forward) it else -it } + fadeIn(effectsSpec)) togetherWith
                        (slideOutHorizontally(spatialSpec) { if (forward) -it / 3 else it / 3 } + fadeOut(effectsSpec))
                },
                label = "navigation",
                modifier = Modifier.fillMaxSize().padding(bottom = bottomInset),
            ) { page ->
                when (page) {
                    Destination.DASHBOARD -> DashboardScreen(state, dailyState.summary,
                        { resolve = true }, dashboard::applyScene,
                        dashboard::adjustUberAr, dashboard::openPlatform)
                    Destination.ANALYSIS -> AnalysisScreen(
                        analysis, dailyState, analysisViewModel::select,
                        dailyReviewViewModel::selectRange, dailyReviewViewModel::selectPlatform,
                        dailyReviewViewModel::selectFilter, dailyReviewViewModel::clearAll,
                    )
                    Destination.RULES -> RulesScreen(rules, rulesSection) { rulesSection = it }
                    Destination.SETTINGS -> SettingsScreen(
                        factory, rules, voice, diagnostics, state, settingsSection,
                        { settingsSection = it }, dashboard::setAutomatic,
                        onOverlayPermission, onNotificationPermission, onLocationPermission,
                    )
                }
            }
            if (!inSubPage) {
                BottomNavigationDock(
                    selected = selected,
                    working = state.working,
                    authorized = bridge.authorized,
                    onSelect = { dest ->
                        if (dest != selected) navigate { selected = dest; rulesSection = null; settingsSection = null }
                    },
                    onWork = onWork,
                    onAuthorize = dashboard::authorize,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
    OverudDialogs(
        hasPendingNav = pendingNavigation != null,
        savingRules = rules.state.value.saving,
        showResolve = resolve,
        onDismissNav = { pendingNavigation = null },
        onDiscardAndNav = {
            rules.discardChanges()
            val action = pendingNavigation
            pendingNavigation = null
            action?.invoke()
        },
        onDismissResolve = { resolve = false },
        onConfirmResolve = { resolve = false; dashboard.resolvePending() },
    )
}
