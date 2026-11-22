package io.github.pennymorant.offerpilot.overlay

import android.content.Context
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

/** iOS Dynamic Island style top-camera floating overlay for live offer alerts and quick actions. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: View? = null
    private var window: OverlayWindowLayout? = null
    private val collapsedOfferId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var lastOfferId: String? = null

    fun show() {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val v = buildOverlayViews(context,
            onPause = { runtime.setAutomatic(false) }, onDecline = { runtime.declineCurrentOffer() })
        val layout = OverlayWindowLayout(context, manager, v)

        v.topRow.setOnClickListener {
            val currentId = lastOfferId ?: return@setOnClickListener
            collapsedOfferId.value = if (collapsedOfferId.value == currentId) null else currentId
        }
        runCatching { layout.attach(); window = layout; view = v.root }.onFailure {
            layout.close()
            scope.cancel()
            return
        }

        scope.launch {
            combine(runtime.state, runtime.appearance.state, collapsedOfferId) { state, appearance, collapsed ->
                Triple(state, appearance, collapsed)
            }.collectLatest { (state, appearance, collapsed) ->
                    val systemDark = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    val colors = appearanceColors(context, appearance, appearance.isDark(systemDark))
                    val offer = state.latest
                    if (offer?.id != lastOfferId) {
                        lastOfferId = offer?.id
                        if (collapsedOfferId.value != null) collapsedOfferId.value = null
                    }
                    val arState = runtime.arGuard.state.value
                    val advice = offer?.let {
                        StrategyAdvisor.advise(it, state.decision, state.destinationAssessment,
                            arState.uberArPercent, state.policy.effective(it.platform).preferredZones)
                    }
                    val alert = advice?.fakePreferred == true
                    val isExpandedOffer = offer != null && collapsed != offer.id
                    val bgAlert = state.backgroundAlert?.takeIf {
                        System.currentTimeMillis() - it.receivedAt in 0..35_000L && it.platform != offer?.platform
                    }
                    val active = state.activePlatform
                    val otherActive = active?.let { p -> Platform.entries.firstOrNull { it != p } }
                    val completed = state.lastCompletedPlatform
                    val otherCompleted = completed?.let { p -> Platform.entries.firstOrNull { it != p } }
                    val recentComplete = state.lastCompletedAt?.let { System.currentTimeMillis() - it in 0..90_000L } == true
                    val hasActivity = (state.deliveryPhase == DeliveryPhase.ACTIVE && otherActive != null) ||
                        (bgAlert != null && state.deliveryPhase == DeliveryPhase.AVAILABLE) ||
                        (state.deliveryPhase == DeliveryPhase.AVAILABLE && recentComplete && otherCompleted != null)
                    layout.update(isExpandedOffer, when {
                        offer != null || hasActivity || state.autoAcceptEnabled -> 236
                        else -> 184
                    })
                    applyIslandColors(v, colors, alert, state.decision?.eligible == true, dp(1))

                    renderOfferContent(v, state, colors, isExpandedOffer, advice, arState.uberArPercent, arState.goldFloorPercent, bgAlert)

                    v.pauseBtn.visibility = if (state.autoAcceptEnabled) View.VISIBLE else View.GONE
                    v.declineBtn.visibility = if (isExpandedOffer && state.decision?.eligible == false &&
                        state.deliveryPhase == DeliveryPhase.AVAILABLE) View.VISIBLE else View.GONE
                    when {
                        state.deliveryPhase == DeliveryPhase.ACTIVE && active != null && otherActive != null -> {
                            v.switchBtn.visibility = View.VISIBLE; v.switchBtn.text = "去暂停 ${otherActive.label}"
                            v.switchBtn.setOnClickListener { runtime.launchOtherPlatform(active) }
                        }
                        bgAlert != null && state.deliveryPhase == DeliveryPhase.AVAILABLE -> {
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
                }
        }
    }

    fun close() {
        scope.cancel()
        window?.close()
        window = null
        view = null
    }
}
