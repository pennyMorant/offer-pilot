package com.overud.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.toArgb
import com.overud.di.AppContainer
import com.overud.model.*
import com.overud.ui.components.money
import com.overud.ui.components.offerAmountPresentation
import com.overud.ui.theme.appearanceColors
import com.overud.ui.theme.isDark
import com.overud.valuation.StrategyAdvisor
import com.overud.valuation.ValueEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.util.Locale
import kotlin.math.roundToLong

/** iOS Dynamic Island style top-camera floating overlay for live offer alerts and quick actions. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: View? = null
    private val collapsedOfferId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var lastOfferId: String? = null

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val metrics = runCatching { manager.currentWindowMetrics }.getOrNull()
        val screenW = metrics?.bounds?.width() ?: context.resources.displayMetrics.widthPixels
        val cutout = runCatching { metrics?.windowInsets?.displayCutout?.boundingRects?.firstOrNull() }.getOrNull()
        val centeredCutout = cutout != null && kotlin.math.abs(cutout.centerX() - screenW / 2) <= screenW / 6
        val cutoutW = if (centeredCutout) cutout!!.width() else dp(8)
        val cutoutH = if (centeredCutout) cutout!!.height() else dp(24)
        val cutoutTop = if (centeredCutout) cutout!!.top.coerceAtLeast(dp(4)) else dp(8)

        val v = buildOverlayViews(context, cutoutW, cutoutH,
            onPause = { runtime.setAutomatic(false) }, onDecline = { runtime.declineCurrentOffer() })
        val params = WindowManager.LayoutParams(
            dp(168), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = cutoutTop
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        v.topRow.setOnClickListener {
            val currentId = lastOfferId ?: return@setOnClickListener
            collapsedOfferId.value = if (collapsedOfferId.value == currentId) null else currentId
        }
        runCatching { manager.addView(v.root, params); view = v.root }.onFailure { scope.cancel() }

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
                    val advice = offer?.let {
                        StrategyAdvisor.advise(it, state.decision, state.destinationAssessment,
                            runtime.arGuard.state.value.uberArPercent, state.policy.effective(it.platform).preferredZones)
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
                    val targetWidth = when {
                        isExpandedOffer -> dp(338)
                        offer != null || hasActivity || state.autoAcceptEnabled -> dp(248)
                        else -> dp(168)
                    }
                    if (params.width != targetWidth) {
                        TransitionManager.beginDelayedTransition(v.root, AutoTransition().apply { duration = 180 })
                        params.width = targetWidth
                        runCatching { manager.updateViewLayout(v.root, params) }
                    }
                    applyIslandColors(v, colors, alert, dp(1))

                    val modeDot = when {
                        state.automatic -> "● 自动"
                        state.autoAcceptEnabled -> "◐ 待命"
                        else -> "● 监测"
                    }
                    if (offer != null) {
                        val score = state.decision?.valuation?.score
                        val scoreText = score?.let { " ${it}分" }.orEmpty()
                        v.leftBadge.stylePill(
                            if (score != null && score >= 70) colors.primaryContainer.toArgb() else Color.rgb(38, 42, 48),
                            if (score != null && score >= 70) colors.onPrimaryContainer.toArgb() else Color.WHITE,
                        )
                        v.leftBadge.text = "${offer.platform.label}$scoreText"
                        val mi = offer.distanceMeters?.takeIf { offer.hasVerifiedDistance() }?.let {
                            String.format(Locale.US, "%.1fmi", it / ValueEngine.METERS_PER_MILE)
                        } ?: "—"
                        val min = offer.durationSeconds?.takeIf { offer.hasVerifiedDuration() }?.let { "${it / 60}m" } ?: "—"
                        v.rightBadge.stylePill(Color.TRANSPARENT, Color.rgb(210, 215, 222))
                        v.rightBadge.text = if (isExpandedOffer) "$min · $mi" else offerAmountPresentation(offer)

                        v.heroRow.visibility = if (isExpandedOffer) View.VISIBLE else View.GONE
                        v.detailText.visibility = if (isExpandedOffer) View.VISIBLE else View.GONE
                        val valuation = state.decision?.valuation
                        v.amountText.text = offerAmountPresentation(offer)
                        v.perMileTile.text = "${valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—"}/mi"
                        val hr = valuation?.contributionCentsPerHour ?: valuation?.grossCentsPerHour
                        v.hourlyTile.text = "${hr?.let { money(it.roundToLong()) } ?: "—"}/h"
                        val eligible = state.decision?.eligible == true
                        v.verdictTile.stylePill(
                            if (eligible) colors.primaryContainer.toArgb() else colors.errorContainer.toArgb(),
                            if (eligible) colors.onPrimaryContainer.toArgb() else colors.onErrorContainer.toArgb(),
                        )
                        v.verdictTile.text = if (eligible) "通过" else "拦截"
                        val tags = advice?.tags?.take(1)?.joinToString("").orEmpty()
                        val place = offer.merchant?.take(18) ?: offer.destination?.take(18) ?: ""
                        val reason = state.decision?.reasons?.firstOrNull() ?: "全部条件通过"
                        val cycle = advice?.cycleCentsPerHour?.let { "周期${money(it.roundToLong())}/h" }.orEmpty()
                        v.detailText.text = listOf(tags, place, cycle, reason).filter(String::isNotBlank).joinToString(" · ")
                    } else {
                        v.heroRow.visibility = View.GONE
                        v.detailText.visibility = View.GONE
                        v.leftBadge.stylePill(
                            if (state.automatic) colors.primaryContainer.toArgb() else Color.TRANSPARENT,
                            if (state.automatic) colors.onPrimaryContainer.toArgb() else Color.rgb(160, 230, 180),
                        )
                        v.leftBadge.text = modeDot
                        v.rightBadge.stylePill(Color.TRANSPARENT, Color.rgb(215, 220, 226))
                        v.rightBadge.text = if (state.deliveryPhase == DeliveryPhase.ACTIVE) "配送中" else "OVERUD"
                    }

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

    fun close() { scope.cancel(); view?.let { runCatching { manager.removeView(it) } }; view = null }
}
