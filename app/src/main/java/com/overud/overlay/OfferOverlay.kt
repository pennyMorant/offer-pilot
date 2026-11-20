package com.overud.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.toArgb
import com.overud.di.AppContainer
import com.overud.model.*
import com.overud.ui.components.*
import com.overud.ui.theme.appearanceColors
import com.overud.ui.theme.isDark
import com.overud.valuation.StrategyAdvisor
import com.overud.valuation.ValueEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Draggable Android 17 M3 Expressive cockpit overlay for live offer decisions. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: View? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val v = buildOverlayViews(context, onPause = { runtime.setAutomatic(false) }, onDecline = { runtime.declineCurrentOffer() })
        val params = WindowManager.LayoutParams(
            dp(264), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.END; y = dp(84); x = dp(10) }

        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f; var moved = false
        v.root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = params.x; startY = params.y; touchX = event.rawX; touchY = event.rawY; moved = false; false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (touchX - event.rawX).toInt(); val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) { moved = true; params.x = startX + dx; params.y = startY + dy; runCatching { manager.updateViewLayout(v.root, params) } }
                    moved
                }
                else -> moved
            }
        }
        runCatching { manager.addView(v.root, params); view = v.root }.onFailure { scope.cancel() }

        scope.launch {
            combine(runtime.state, runtime.appearance.state) { state, appearance -> state to appearance }
                .collectLatest { (state, appearance) ->
                    val systemDark = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    val colors = appearanceColors(context, appearance, appearance.isDark(systemDark))
                    val offer = state.latest
                    val advice = offer?.let {
                        StrategyAdvisor.advise(it, state.decision, state.destinationAssessment,
                            runtime.arGuard.state.value.uberArPercent, state.policy.effective(it.platform).preferredZones)
                    }
                    val alert = advice?.fakePreferred == true
                    applyBaseColors(v, colors, alert, dp(1))

                    val mode = when {
                        state.automatic -> "自动接单中"
                        state.autoAcceptEnabled -> "自动待命"
                        else -> "手动监测"
                    }
                    v.modeBadge.stylePill(
                        if (state.automatic) colors.primaryContainer.toArgb() else colors.surfaceContainerHigh.toArgb(),
                        if (state.automatic) colors.onPrimaryContainer.toArgb() else colors.onSurfaceVariant.toArgb(),
                    )
                    v.modeBadge.text = if (offer != null) "${offer.platform.label} · $mode" else "OVERUD · $mode"

                    if (offer != null) {
                        v.heroRow.visibility = View.VISIBLE
                        v.metricsRow.visibility = View.VISIBLE
                        val valuation = state.decision?.valuation
                        val score = valuation?.score
                        v.scoreBadge.visibility = if (score != null) View.VISIBLE else View.GONE
                        if (score != null) {
                            val (sBg, sFg) = when {
                                score >= 70 -> colors.primaryContainer.toArgb() to colors.onPrimaryContainer.toArgb()
                                score >= 50 -> colors.tertiaryContainer.toArgb() to colors.onTertiaryContainer.toArgb()
                                else -> colors.errorContainer.toArgb() to colors.onErrorContainer.toArgb()
                            }
                            v.scoreBadge.stylePill(sBg, sFg)
                            v.scoreBadge.text = "${score}分"
                        }
                        v.amountText.text = offerAmountPresentation(offer)
                        val mi = offer.distanceMeters?.takeIf { offer.hasVerifiedDistance() }?.let {
                            String.format(Locale.US, "%.1fmi", it / ValueEngine.METERS_PER_MILE)
                        } ?: "—"
                        val min = offer.durationSeconds?.takeIf { offer.hasVerifiedDuration() }?.let { "${it / 60}m" } ?: "—"
                        v.routeText.text = "$min · $mi"
                        v.perMileTile.text = "每英里\n${valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—"}"
                        val hr = valuation?.contributionCentsPerHour ?: valuation?.grossCentsPerHour
                        v.hourlyTile.text = "时薪\n${hr?.let { money(it.roundToLong()) } ?: "—"}"
                        val eligible = state.decision?.eligible == true
                        v.verdictTile.stylePill(
                            if (eligible) colors.primaryContainer.toArgb() else colors.errorContainer.toArgb(),
                            if (eligible) colors.onPrimaryContainer.toArgb() else colors.onErrorContainer.toArgb(),
                        )
                        v.verdictTile.text = "判断\n${if (eligible) "通过" else "拦截"}"
                        val tags = advice?.tags?.take(2)?.joinToString(" · ").orEmpty()
                        val reason = state.decision?.reasons?.firstOrNull() ?: "全部条件通过"
                        val cycle = advice?.cycleCentsPerHour?.let { " · 等单周期 ${money(it.roundToLong())}/h" }.orEmpty()
                        val prop = prop22Compact(valuation?.prop22).trim()
                        v.detailText.text = listOf(tags, "$reason$cycle", prop).filter(String::isNotBlank).joinToString("\n")
                    } else {
                        v.heroRow.visibility = View.GONE
                        v.metricsRow.visibility = View.GONE
                        v.scoreBadge.visibility = View.GONE
                        v.detailText.text = platformPagePresentation(state.currentPage) ?: "等待新报价…"
                    }

                    v.pauseBtn.visibility = if (state.autoAcceptEnabled) View.VISIBLE else View.GONE
                    v.declineBtn.visibility = if (offer != null && state.decision?.eligible == false &&
                        state.deliveryPhase == DeliveryPhase.AVAILABLE) View.VISIBLE else View.GONE
                    val active = state.activePlatform
                    val otherActive = active?.let { p -> Platform.entries.firstOrNull { it != p } }
                    val completed = state.lastCompletedPlatform
                    val otherCompleted = completed?.let { p -> Platform.entries.firstOrNull { it != p } }
                    val recentComplete = state.lastCompletedAt?.let { System.currentTimeMillis() - it in 0..90_000L } == true
                    val bgAlert = state.backgroundAlert?.takeIf {
                        System.currentTimeMillis() - it.receivedAt in 0..35_000L && it.platform != offer?.platform
                    }
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
