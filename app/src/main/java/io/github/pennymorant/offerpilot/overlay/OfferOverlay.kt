package io.github.pennymorant.offerpilot.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import io.github.pennymorant.offerpilot.MainActivity
import io.github.pennymorant.offerpilot.di.AppContainer
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.ui.theme.appearanceColors
import io.github.pennymorant.offerpilot.ui.theme.isDark
import io.github.pennymorant.offerpilot.valuation.StrategyAdvisor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlin.math.abs

/** iOS Dynamic Island style top-camera floating overlay for live offer alerts and quick actions. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: View? = null
    private var window: OverlayWindowLayout? = null
    private val collapsedOfferId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var lastOfferId: String? = null
    private var expandedNow = false
    private var timeoutJob: Job? = null

    fun show() {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val v = buildOverlayViews(context,
            onPause = { runtime.setAutomatic(false) },
            onDecline = {
                runtime.declineCurrentOffer()
                lastOfferId?.let { collapsedOfferId.value = it }
            })
        val layout = OverlayWindowLayout(context, manager, v)

        v.closeBtn.setOnClickListener {
            lastOfferId?.let { collapsedOfferId.value = it }
        }
        v.topRow.setOnClickListener {
            if (expandedNow) {
                lastOfferId?.let { collapsedOfferId.value = it }
            } else {
                openApp()
            }
        }
        var downX = 0f
        var downY = 0f
        v.root.setOnTouchListener { _, event ->
            if (!expandedNow) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; false }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(48) || dy < -dp(36)) {
                        lastOfferId?.let { collapsedOfferId.value = it }
                        true
                    } else false
                }
                else -> false
            }
        }
        v.root.visibility = View.GONE
        runCatching { layout.attach(); window = layout; view = v.root }.onFailure {
            layout.close()
            scope.cancel()
            return
        }

        scope.launch {
            combine(runtime.state, runtime.appearance.state, collapsedOfferId, runtime.appInForeground) {
                state, appearance, collapsed, inApp -> Quad(state, appearance, collapsed, inApp)
            }.collectLatest { (state, appearance, collapsed, inApp) ->
                if (inApp) {
                    v.root.visibility = View.GONE
                    return@collectLatest
                }
                v.root.visibility = View.VISIBLE
                val systemDark = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                val colors = appearanceColors(context, appearance, appearance.isDark(systemDark))
                val offer = state.latest
                if (offer?.id != lastOfferId) {
                    lastOfferId = offer?.id
                    if (collapsedOfferId.value != null) collapsedOfferId.value = null
                    timeoutJob?.cancel()
                    if (offer != null) {
                        timeoutJob = scope.launch {
                            delay(30_000L)
                            if (lastOfferId == offer.id) collapsedOfferId.value = offer.id
                        }
                    }
                }
                val arState = runtime.arGuard.state.value
                val advice = offer?.let {
                    StrategyAdvisor.advise(it, state.decision, state.destinationAssessment,
                        arState.uberArPercent, state.policy.effective(it.platform).preferredZones)
                }
                val alert = advice?.fakePreferred == true
                val isExpandedOffer = offer != null && collapsed != offer.id
                expandedNow = isExpandedOffer
                if (!isExpandedOffer) {
                    v.root.visibility = View.GONE
                    return@collectLatest
                }
                v.root.visibility = View.VISIBLE
                val bgAlert = state.backgroundAlert?.takeIf {
                    System.currentTimeMillis() - it.receivedAt in 0..35_000L && it.platform != offer?.platform
                }
                val active = state.activePlatform
                val otherActive = active?.let { p -> Platform.entries.firstOrNull { it != p } }
                val completed = state.lastCompletedPlatform
                val otherCompleted = completed?.let { p -> Platform.entries.firstOrNull { it != p } }
                val recentComplete = state.lastCompletedAt?.let { System.currentTimeMillis() - it in 0..90_000L } == true
                layout.update(true, 108)
                applyIslandColors(v, colors, alert, state.decision?.eligible == true, dp(1))

                renderOfferContent(v, state, colors, isExpandedOffer, advice, arState.uberArPercent, arState.goldFloorPercent, bgAlert)

                v.pauseBtn.visibility = if (isExpandedOffer && state.autoAcceptEnabled) View.VISIBLE else View.GONE
                v.declineBtn.visibility = if (isExpandedOffer && state.decision?.eligible == false) View.VISIBLE else View.GONE
                when {
                    isExpandedOffer && state.deliveryPhase == DeliveryPhase.ACTIVE && active != null && otherActive != null -> {
                        v.switchBtn.visibility = View.VISIBLE; v.switchBtn.text = "去暂停 ${otherActive.label}"
                        v.switchBtn.setOnClickListener { runtime.launchOtherPlatform(active) }
                    }
                    isExpandedOffer && bgAlert != null -> {
                        v.switchBtn.visibility = View.VISIBLE
                        v.switchBtn.text = "${bgAlert.platform.label}${bgAlert.amountPreview?.let { " $it" }.orEmpty()} · 查看"
                        v.switchBtn.setOnClickListener { runtime.launchPlatform(bgAlert.platform) }
                    }
                    isExpandedOffer && state.deliveryPhase == DeliveryPhase.AVAILABLE && recentComplete && completed != null && otherCompleted != null -> {
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

    private fun openApp() {
        runCatching {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
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
