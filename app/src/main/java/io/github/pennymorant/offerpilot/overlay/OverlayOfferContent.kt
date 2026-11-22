package io.github.pennymorant.offerpilot.overlay

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.ui.components.money
import io.github.pennymorant.offerpilot.ui.components.offerAmountPresentation
import io.github.pennymorant.offerpilot.valuation.StrategyAdvice
import io.github.pennymorant.offerpilot.valuation.ValueEngine
import io.github.pennymorant.offerpilot.work.WorkState
import java.util.Locale
import kotlin.math.roundToLong

internal fun renderOfferContent(
    v: OverlayViews,
    state: WorkState,
    colors: ColorScheme,
    isExpandedOffer: Boolean,
    advice: StrategyAdvice?,
    uberArPercent: Int,
    goldFloorPercent: Int,
    bgAlert: BackgroundOfferAlert?,
) {
    val offer = state.latest
    val alert = advice?.fakePreferred == true
    val modeDot = when {
        state.automatic -> "● 自动"
        state.autoAcceptEnabled -> "◐ 待命"
        else -> "● 监测"
    }
    if (isExpandedOffer && offer != null) {
        v.statusHalo.visibility = View.GONE
        v.appLogo.visibility = View.GONE
        v.leftBadge.visibility = View.VISIBLE
        v.rightBadge.visibility = View.VISIBLE
        v.closeBtn.visibility = View.VISIBLE
        val score = state.decision?.valuation?.score
        val scoreText = score?.let { " ${it}分" }.orEmpty()
        v.leftBadge.stylePill(
            if (score != null && score >= 70) colors.primaryContainer.toArgb() else Color.rgb(38, 42, 48),
            if (score != null && score >= 70) colors.onPrimaryContainer.toArgb() else Color.WHITE,
        )
        v.leftBadge.text = "${offer.platform.label}$scoreText"
        val mi = offer.distanceMeters?.takeIf { offer.hasVerifiedDistance() }?.let {
            String.format(Locale.US, "%.1f mi", it / ValueEngine.METERS_PER_MILE)
        } ?: "—"
        val min = offer.durationSeconds?.takeIf { offer.hasVerifiedDuration() }?.let { "${it / 60} min" } ?: "—"
        v.rightBadge.stylePill(Color.TRANSPARENT, Color.rgb(176, 182, 190))
        v.rightBadge.text = modeDot

        v.heroRow.visibility = View.VISIBLE
        v.metricsRow.visibility = View.VISIBLE
        val valuation = state.decision?.valuation
        v.amountText.text = offerAmountPresentation(offer)
        v.heroRouteText.text = "$min · $mi"
        v.perMileTile.text = "${valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—"}/mi"
        val hr = valuation?.contributionCentsPerHour ?: valuation?.grossCentsPerHour
        v.hourlyTile.text = "${hr?.let { money(it.roundToLong()) } ?: "—"}/h"
        val eligible = state.decision?.eligible == true
        v.verdictTile.stylePill(
            if (eligible) colors.primaryContainer.toArgb() else colors.errorContainer.toArgb(),
            if (eligible) colors.onPrimaryContainer.toArgb() else colors.onErrorContainer.toArgb(),
        )
        v.verdictTile.text = if (eligible) "通过" else "拦截"
        val tags = advice?.tags?.take(3)?.joinToString(" · ").orEmpty()
        val routePlaces = listOfNotNull(
            offer.merchant?.take(22) ?: offer.pickupAddress?.take(22),
            offer.destination?.take(24),
        ).joinToString(" → ")
        val reason = state.decision?.reasons?.firstOrNull() ?: "全部条件通过"
        val notes = valuation?.scoreNotes?.take(2)?.joinToString(" · ").orEmpty()
        val verdictLine = listOf(reason, notes).filter(String::isNotBlank).joinToString(" · ")
        val cycle = advice?.cycleCentsPerHour?.let { "含等单周期 ${money(it.roundToLong())}/h" }.orEmpty()
        val prop = io.github.pennymorant.offerpilot.ui.components.prop22Compact(valuation?.prop22).trim()
        val destHint = io.github.pennymorant.offerpilot.ui.components.destinationPresentation(state.destinationAssessment, Locale.getDefault()).title
        val extraLine = listOf(cycle, prop, destHint).filter(String::isNotBlank).joinToString(" · ")
        v.tagsText.visibility = if (tags.isNotBlank()) View.VISIBLE else View.GONE
        v.tagsText.text = tags
        v.routeText.visibility = if (routePlaces.isNotBlank()) View.VISIBLE else View.GONE
        v.routeText.text = routePlaces
        v.infoBox.visibility = View.VISIBLE
        v.verdictText.text = if (eligible) "✓ $verdictLine" else "• $verdictLine"
        v.extraText.visibility = if (extraLine.isNotBlank()) View.VISIBLE else View.GONE
        v.extraText.text = extraLine
    } else {
        v.statusHalo.visibility = View.VISIBLE
        v.appLogo.visibility = View.VISIBLE
        v.leftBadge.visibility = View.GONE
        v.rightBadge.visibility = View.GONE
        v.closeBtn.visibility = View.GONE
        v.heroRow.visibility = View.GONE
        v.metricsRow.visibility = View.GONE
        v.tagsText.visibility = View.GONE
        v.routeText.visibility = View.GONE
        v.infoBox.visibility = View.GONE
        val dotColor = when {
            state.automatic -> colors.primary.toArgb()
            state.deliveryPhase == DeliveryPhase.ACTIVE -> Color.rgb(251, 191, 36)
            bgAlert != null -> colors.tertiary.toArgb()
            else -> Color.rgb(74, 222, 128)
        }
        (v.statusDot.background as? GradientDrawable)?.setColor(dotColor)
        val haloColor = Color.argb(72, Color.red(dotColor), Color.green(dotColor), Color.blue(dotColor))
        (v.statusHalo.background as? GradientDrawable)?.setColor(haloColor)
    }
}
