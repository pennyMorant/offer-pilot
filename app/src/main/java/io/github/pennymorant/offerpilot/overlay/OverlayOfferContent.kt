package io.github.pennymorant.offerpilot.overlay

import android.graphics.Color
import android.view.View
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.ui.components.destinationPresentation
import io.github.pennymorant.offerpilot.ui.components.money
import io.github.pennymorant.offerpilot.ui.components.offerAmountPresentation
import io.github.pennymorant.offerpilot.ui.components.prop22Compact
import io.github.pennymorant.offerpilot.valuation.StrategyAdvice
import io.github.pennymorant.offerpilot.valuation.ValueEngine
import io.github.pennymorant.offerpilot.work.WorkState
import java.util.Locale
import kotlin.math.roundToLong

internal fun renderOfferContent(
    v: OverlayViews,
    state: WorkState,
    colors: ColorScheme,
    offer: Offer,
    advice: StrategyAdvice?,
) {
    val score = state.decision?.valuation?.score
    val scoreText = score?.let { " · ${it}分" }.orEmpty()
    val highScore = score != null && score >= 70
    v.leftBadge.stylePill(
        if (highScore) colors.primaryContainer.toArgb() else colors.surfaceContainerHighest.toArgb(),
        if (highScore) colors.onPrimaryContainer.toArgb() else colors.onSurfaceVariant.toArgb(),
    )
    v.leftBadge.text = "${offer.platform.label}$scoreText"

    val modeLabel = when {
        state.automatic -> "自动接单"
        state.autoAcceptEnabled -> "自动待命"
        else -> "实时监测"
    }
    v.rightBadge.stylePill(Color.TRANSPARENT, colors.onSurfaceVariant.toArgb())
    v.rightBadge.text = modeLabel

    val mi = offer.distanceMeters?.takeIf { offer.hasVerifiedDistance() }?.let {
        String.format(Locale.US, "%.1f mi", it / ValueEngine.METERS_PER_MILE)
    } ?: "—"
    val min = offer.durationSeconds?.takeIf { offer.hasVerifiedDuration() }?.let { "${it / 60} min" } ?: "—"
    val valuation = state.decision?.valuation
    val eligible = state.decision?.eligible == true

    v.amountText.text = offerAmountPresentation(offer)
    v.heroRouteText.text = "$min · $mi"
    v.verdictTile.stylePill(
        if (eligible) colors.primary.toArgb() else colors.surfaceContainerHighest.toArgb(),
        if (eligible) colors.onPrimary.toArgb() else colors.onSurfaceVariant.toArgb(),
    )
    v.verdictTile.text = if (eligible) "✓ 通过" else "拦截"

    v.perMileTile.text = "${valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—"}/mi"
    val hr = valuation?.contributionCentsPerHour ?: valuation?.grossCentsPerHour
    v.hourlyTile.text = "${hr?.let { money(it.roundToLong()) } ?: "—"}/h"

    val tags = advice?.tags?.take(3)?.joinToString(" · ").orEmpty()
    val routePlaces = listOfNotNull(
        offer.merchant?.take(22) ?: offer.pickupAddress?.take(22),
        offer.destination?.take(24),
    ).joinToString(" → ")
    val reason = state.decision?.reasons?.firstOrNull() ?: "全部启用条件通过"
    val notes = valuation?.scoreNotes?.take(2)?.joinToString(" · ").orEmpty()
    val verdictLine = listOf(reason, notes).filter(String::isNotBlank).joinToString(" · ")
    val cycle = advice?.cycleCentsPerHour?.let { "含等单周期 ${money(it.roundToLong())}/h" }.orEmpty()
    val prop = prop22Compact(valuation?.prop22).trim()
    val destHint = destinationPresentation(state.destinationAssessment, Locale.getDefault()).title
    val extraLine = listOf(cycle, prop, destHint).filter(String::isNotBlank).joinToString(" · ")

    v.tagsText.visibility = if (tags.isNotBlank()) View.VISIBLE else View.GONE
    v.tagsText.text = tags
    v.routeText.visibility = if (routePlaces.isNotBlank()) View.VISIBLE else View.GONE
    v.routeText.text = routePlaces
    v.verdictText.text = if (eligible) "✓ $verdictLine" else "• $verdictLine"
    v.extraText.visibility = if (extraLine.isNotBlank()) View.VISIBLE else View.GONE
    v.extraText.text = extraLine
}
