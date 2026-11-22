package io.github.pennymorant.offerpilot.overlay

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import io.github.pennymorant.offerpilot.di.AppContainer
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.ui.theme.appearanceColors
import io.github.pennymorant.offerpilot.ui.theme.isDark
import io.github.pennymorant.offerpilot.valuation.StrategyAdvisor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

/** Material 3 Expressive dynamic-color floating offer card covering the top status bar on live offers. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: View? = null
    private var window: OverlayWindowLayout? = null
    private val collapsedOfferId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var lastOfferId: String? = null
    private var timeoutJob: Job? = null
    private var dismissing = false

    fun show() {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        var layoutRef: OverlayWindowLayout? = null
        fun dismissCurrent() {
            val targetId = lastOfferId ?: return
            if (dismissing) return
            dismissing = true
            val l = layoutRef
            if (l != null) {
                l.animateOut {
                    dismissing = false
                    collapsedOfferId.value = targetId
                }
            } else {
                dismissing = false
                collapsedOfferId.value = targetId
            }
        }
        val v = buildOverlayViews(context,
            onPause = { runtime.setAutomatic(false) },
            onDecline = {
                runtime.declineCurrentOffer()
                dismissCurrent()
            },
            onSwipeUp = ::dismissCurrent)
        val layout = OverlayWindowLayout(context, manager, v)
        layoutRef = layout

        v.closeBtn.setOnClickListener { dismissCurrent() }
        v.root.visibility = View.GONE
        runCatching { layout.attach(); window = layout; view = v.root }.onFailure {
            layout.close(); scope.cancel(); return
        }

        scope.launch {
            combine(runtime.state, runtime.appearance.state, collapsedOfferId, runtime.appInForeground) {
                state, appearance, collapsed, inApp -> Quad(state, appearance, collapsed, inApp)
            }.collectLatest { (state, appearance, collapsed, inApp) ->
                val offer = state.latest
                val isDemo = offer?.id?.startsWith("demo-") == true
                if (offer == null || (inApp && !isDemo)) {
                    v.root.visibility = View.GONE
                    return@collectLatest
                }
                if (offer.id != lastOfferId) {
                    lastOfferId = offer.id
                    dismissing = false
                    if (collapsedOfferId.value != null) collapsedOfferId.value = null
                    timeoutJob?.cancel()
                    timeoutJob = scope.launch {
                        delay(30_000L)
                        if (lastOfferId == offer.id) dismissCurrent()
                    }
                }
                if (collapsed == offer.id || dismissing) {
                    if (!dismissing) v.root.visibility = View.GONE
                    return@collectLatest
                }
                val wasHidden = v.root.visibility != View.VISIBLE
                val systemDark = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val dark = appearance.isDark(systemDark)
                val colors = appearanceColors(context, appearance, dark)
                val arState = runtime.arGuard.state.value
                val advice = StrategyAdvisor.advise(offer, state.decision, state.destinationAssessment,
                    arState.uberArPercent, state.policy.effective(offer.platform).preferredZones)
                val bgAlert = state.backgroundAlert?.takeIf {
                    System.currentTimeMillis() - it.receivedAt in 0..35_000L && it.platform != offer.platform
                }
                val active = state.activePlatform
                val otherActive = active?.let { p -> Platform.entries.firstOrNull { it != p } }
                val completed = state.lastCompletedPlatform
                val otherCompleted = completed?.let { p -> Platform.entries.firstOrNull { it != p } }
                val recentComplete = state.lastCompletedAt?.let { System.currentTimeMillis() - it in 0..90_000L } == true

                applyExpressiveColors(v, colors, dark, state.decision?.eligible == true, dp(1))
                renderOfferContent(v, state, colors, offer, advice)

                v.pauseBtn.visibility = if (state.autoAcceptEnabled) View.VISIBLE else View.GONE
                v.declineBtn.visibility = if (state.decision?.eligible == false) View.VISIBLE else View.GONE
                when {
                    state.deliveryPhase == DeliveryPhase.ACTIVE && active != null && otherActive != null -> {
                        v.switchBtn.visibility = View.VISIBLE; v.switchBtn.text = "去暂停 ${otherActive.label}"
                        v.switchBtn.setOnClickListener { runtime.launchOtherPlatform(active) }
                    }
                    bgAlert != null -> {
                        v.switchBtn.visibility = View.VISIBLE
                        v.switchBtn.text = "${bgAlert.platform.label}${bgAlert.amountPreview?.let { " $it" }.orEmpty()} · 查看"
                        v.switchBtn.setOnClickListener { runtime.launchPlatform(bgAlert.platform) }
                    }
                    state.deliveryPhase == DeliveryPhase.AVAILABLE && recentComplete && completed != null && otherCompleted != null -> {
                        v.switchBtn.visibility = View.VISIBLE; v.switchBtn.text = "去恢复 ${otherCompleted.label}"
                        v.switchBtn.setOnClickListener { runtime.launchOtherPlatform(completed) }
                    }
                    else -> v.switchBtn.visibility = View.GONE
                }
                v.actionsRow.visibility = if (v.pauseBtn.visibility == View.VISIBLE ||
                    v.declineBtn.visibility == View.VISIBLE || v.switchBtn.visibility == View.VISIBLE) View.VISIBLE else View.GONE

                layout.refresh()
                v.root.visibility = View.VISIBLE
                if (wasHidden) layout.animateIn()
            }
        }
    }

    fun close() {
        timeoutJob?.cancel()
        scope.cancel()
        window?.close()
        window = null
        view = null
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
