package io.github.pennymorant.offerpilot.ui.components

import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.valuation.ValueEngine
import java.util.Locale

fun offerAmountPresentation(offer: Offer): String {
    val cents = offer.amountCents ?: return "金额待识别"
    if (offer.currency == "USD") return money(cents)
    val unit = if (offer.currency == "UNKNOWN") "币种待确认" else offer.currency
    return "$unit ${String.format(Locale.US, "%.2f", cents / 100.0)}"
}

fun offerRoutePresentation(offer: Offer): String {
    val distanceLabel = if (!offer.hasVerifiedDistance()) "里程口径未确认"
        else if (offer.isAddon) "新增里程" else "总里程"
    val timeLabel = if (!offer.hasVerifiedDuration()) "耗时口径未确认"
        else if (offer.isAddon) "新增耗时" else "总耗时"
    val distance = offer.distanceMeters?.let { String.format(Locale.US, "%.1f mi", it / ValueEngine.METERS_PER_MILE) } ?: "未知"
    val duration = offer.durationSeconds?.let { "${it / 60} min" } ?: "未知"
    return "$distanceLabel $distance · $timeLabel $duration"
}
