package com.overud.analytics

import com.overud.data.database.projection.DeliverySummary
import com.overud.model.DurationCalibration
import com.overud.model.Offer
import com.overud.model.OfferService
import com.overud.model.RegionalEvidenceScope
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

object DeliveryCalibrationSelector {
    fun select(
        offer: Offer,
        region: String?,
        history: List<DeliverySummary>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DurationCalibration? {
        if (region.isNullOrBlank() || offer.serviceKind == OfferService.UNKNOWN) return null
        val local = Instant.ofEpochMilli(offer.observedAt).atZone(zone)
        val base = history.filter {
            it.measurementVersion == DeliveryMeasurement.VERSION &&
                it.platform == offer.platform.name && it.serviceKind == offer.serviceKind.name &&
                it.regionKey == region && it.zoneId == zone.id && it.pairedCount > 0
        }
        val candidates = listOf(
            RegionalEvidenceScope.SAME_HOUR to base.filter {
                it.weekday == local.dayOfWeek.value && it.localHour == local.hour
            },
            RegionalEvidenceScope.ADJACENT_HOURS to base.filter {
                it.weekday == local.dayOfWeek.value && circularHourDistance(it.localHour, local.hour) <= 1
            },
            RegionalEvidenceScope.SAME_HOUR_OTHER_DAYS to base.filter { it.localHour == local.hour },
            RegionalEvidenceScope.REGION_BASELINE to base,
        )
        return candidates.firstNotNullOfOrNull { (scope, rows) -> calibration(rows, scope) }
    }

    private fun calibration(
        rows: List<DeliverySummary>,
        scope: RegionalEvidenceScope,
    ): DurationCalibration? {
        val count = rows.sumOf { it.pairedCount }
        val quoted = rows.sumOf { it.pairedQuotedMillisSum }
        val observed = rows.sumOf { it.pairedObservedMillisSum }
        if (count < DurationCalibration.MINIMUM_SAMPLES || quoted <= 0 || observed <= 0) return null
        return DurationCalibration(observed.toDouble() / quoted, count, scope)
            .takeIf(DurationCalibration::valid)
    }

    private fun circularHourDistance(first: Int, second: Int): Int {
        val direct = abs(first - second)
        return minOf(direct, 24 - direct)
    }
}
