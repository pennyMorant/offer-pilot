package io.github.pennymorant.offerpilot.overlay

import android.view.View
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.ui.components.money
import io.github.pennymorant.offerpilot.ui.components.offerAmountPresentation
import io.github.pennymorant.offerpilot.valuation.ValueEngine
import io.github.pennymorant.offerpilot.work.WorkState
import java.util.Locale
import kotlin.math.roundToLong

internal fun renderOfferContent(v: OverlayViews, state: WorkState, offer: Offer) {
    val decision = state.decision
    val valuation = decision?.valuation
    val eligible = decision?.eligible == true
    val score = valuation?.score

    v.verdictBadge.text = if (eligible) "✓ 建议接" else "跳过"
    if (score != null) {
        v.scoreBadge.visibility = View.VISIBLE
        v.scoreBadge.text = "${score}分"
    } else {
        v.scoreBadge.visibility = View.GONE
    }

    val reason = decision?.reasons?.firstOrNull()
    v.reasonText.text = when {
        eligible -> valuation?.scoreNotes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "全部门槛达标"
        !reason.isNullOrBlank() -> reason
        else -> "未达到接单门槛"
    }

    v.amountValue.text = offerAmountPresentation(offer)
    v.perMileValue.text = valuation?.centsPerMile?.let { "${money(it.roundToLong())}/mi" } ?: "—"
    val hr = valuation?.contributionCentsPerHour ?: valuation?.grossCentsPerHour
    v.hourlyValue.text = hr?.let { "${money(it.roundToLong())}/h" } ?: "—"

    val min = offer.durationSeconds?.takeIf { offer.hasVerifiedDuration() }?.let { "${it / 60}m" }
    val mi = offer.distanceMeters?.takeIf { offer.hasVerifiedDistance() }?.let {
        String.format(Locale.US, "%.1fmi", it / ValueEngine.METERS_PER_MILE)
    }
    val place = listOfNotNull(
        offer.merchant?.take(18) ?: offer.pickupAddress?.take(18),
        offer.destination?.take(20),
    ).joinToString(" → ").takeIf { it.isNotBlank() }

    v.routeLine.text = listOfNotNull(offer.platform.label, min, mi, place).joinToString(" · ")
}
