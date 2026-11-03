package com.overud.analytics

import com.overud.model.*
import com.overud.region.RegionCell
import java.time.Instant
import java.time.ZoneId

/** Historical quality evidence, not a calibrated probability of receiving another offer. */
object DestinationEvaluator {
    private data class Candidate(val scope: RegionalEvidenceScope, val rows: List<RegionalQualityObservation>, val minimum: Long)

    fun assess(
        destination: ResolvedDestination,
        platform: Platform,
        arrivalAt: Long?,
        history: List<RegionalQualityObservation>,
        zone: ZoneId = ZoneId.systemDefault(),
        waitingHistory: List<WaitingObservation> = emptyList(),
        serviceKind: OfferService = OfferService.UNKNOWN,
    ): DestinationAssessment {
        val region = destination.regionKey
        val waiting = if (destination.status == DestinationResolution.RESOLVED && region != null)
            WaitingEvaluator.assess(region, platform, arrivalAt, waitingHistory, zone) else null
        val base = DestinationAssessment(destination.status, region, destination.precision, arrivalAt,
            analysisZoneId = zone.id, waiting = waiting)
        if (destination.status != DestinationResolution.RESOLVED || region == null) return base
        val compatible = history.filter { it.platform == platform && it.serviceKind == serviceKind && it.version == QualityEvaluator.VERSION && it.zoneId == zone.id }
        val exactRegion = compatible.filter { it.region == region }
        val local = arrivalAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val candidates = buildList {
            if (local != null) {
                add(Candidate(RegionalEvidenceScope.SAME_HOUR, exactRegion.filter {
                    it.weekday == local.dayOfWeek.value && it.hour == local.hour
                }, 10))
                val adjacent = (-1L..1L).map { local.plusHours(it) }.map { it.dayOfWeek.value to it.hour }.toSet()
                add(Candidate(RegionalEvidenceScope.ADJACENT_HOURS,
                    exactRegion.filter { (it.weekday to it.hour) in adjacent }, 15))
                add(Candidate(RegionalEvidenceScope.SAME_HOUR_OTHER_DAYS, exactRegion.filter { it.hour == local.hour }, 20))
                val nearby = RegionCell.parse(region)?.neighborhood().orEmpty()
                add(Candidate(RegionalEvidenceScope.NEIGHBORHOOD, compatible.filter {
                    it.region in nearby && it.weekday == local.dayOfWeek.value && it.hour == local.hour
                }, 20))
            }
            add(Candidate(RegionalEvidenceScope.REGION_BASELINE, exactRegion, 30))
        }
        val selected = candidates.firstOrNull {
            val classified = it.rows.sumOf(RegionalQualityObservation::classified)
            val observed = it.rows.sumOf(RegionalQualityObservation::observed)
            classified >= it.minimum && classified * 2 >= observed
        }
        if (selected == null) {
            return base.copy(evidenceScope = RegionalEvidenceScope.REGION_BASELINE,
                observedCount = exactRegion.sumOf { it.observed }, classifiedCount = exactRegion.sumOf { it.classified })
        }
        val classified = selected.rows.sumOf { it.classified }
        val good = selected.rows.sumOf { it.good }.toDouble() / classified
        val low = selected.rows.sumOf { it.low }.toDouble() / classified
        val outlook = when {
            good >= 0.5 && low <= 0.25 -> RegionalOutlook.GOOD
            low >= 0.5 -> RegionalOutlook.WEAK
            else -> RegionalOutlook.MIXED
        }
        return base.copy(outlook = outlook, evidenceScope = selected.scope,
            observedCount = selected.rows.sumOf { it.observed }, classifiedCount = classified, goodRate = good)
    }
}
