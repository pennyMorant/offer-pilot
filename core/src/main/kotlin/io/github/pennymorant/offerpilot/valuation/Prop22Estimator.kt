package io.github.pennymorant.offerpilot.valuation

import io.github.pennymorant.offerpilot.model.*
import java.time.Instant
import java.time.ZoneId

/** A standalone offer scenario, never a prediction of the platform's period adjustment. */
object Prop22Estimator {
    const val RATE_YEAR = 2026
    const val MILEAGE_CENTS = 37
    private val california = ZoneId.of("America/Los_Angeles")

    fun calculate(offer: Offer, settings: Prop22Policy): Prop22Estimate? {
        if (!settings.enabled) return null
        fun unknown(status: Prop22Status) = Prop22Estimate(status, settings.minimumWageCents, settings.assumedTipPercent)
        if (!settings.valid()) return unknown(Prop22Status.INVALID_SETTINGS)
        if (Instant.ofEpochMilli(offer.observedAt).atZone(california).year != RATE_YEAR) {
            return unknown(Prop22Status.RATE_YEAR_UNSUPPORTED)
        }
        if (offer.currency != "USD" || offer.isAddon || offer.isStacked) return unknown(Prop22Status.UNSUPPORTED_OFFER)
        val seconds = offer.durationSeconds?.takeIf { it in 1..86400 && offer.hasVerifiedDuration() }
            ?: return unknown(Prop22Status.MISSING_ROUTE)
        val mileage = if (settings.motorVehicle) {
            val meters = offer.distanceMeters?.takeIf { it in 1..1_000_000 && offer.hasVerifiedDistance() }
                ?: return unknown(Prop22Status.MISSING_ROUTE)
            meters / ValueEngine.METERS_PER_MILE * MILEAGE_CENTS
        } else 0.0
        val time = seconds / 3600.0 * settings.minimumWageCents * 1.2
        val floor = time + mileage
        val result = unknown(Prop22Status.MISSING_AMOUNT).copy(timeFloorCents = time,
            mileageFloorCents = mileage, floorCents = floor)
        val amount = offer.amountCents?.takeIf { it in 1..10_000_000 &&
            (offer.reliable || offer.evidence?.amountVerified == true) } ?: return result
        val tips = amount * settings.assumedTipPercent / 100.0
        val platformPay = amount - tips
        val extra = (floor - platformPay).coerceAtLeast(0.0)
        return result.copy(status = Prop22Status.ESTIMATED, assumedPlatformPayCents = platformPay,
            assumedTipCents = tips, extraCents = extra, totalCents = amount + extra)
    }
}
