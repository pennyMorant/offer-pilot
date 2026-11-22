package io.github.pennymorant.offerpilot.analytics

import io.github.pennymorant.offerpilot.model.*
import java.time.Instant
import java.time.ZoneId

/** Conditional observed waits only. Interrupted intervals are never treated as arrivals. */
object WaitingEvaluator {
    const val VERSION = 1
    val BOUNDS = listOf(60_000L, 180_000L, 300_000L, 600_000L, 1_200_000L, 1_800_000L)
    private data class Candidate(val scope: RegionalEvidenceScope, val rows: List<WaitingObservation>, val minimum: Long)

    fun assess(region: String, platform: Platform, arrivalAt: Long?, history: List<WaitingObservation>, zone: ZoneId): WaitingAssessment {
        val compatible = history.filter { it.platform == platform && it.region == region &&
            it.zoneId == zone.id && it.version == VERSION }
        val local = arrivalAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val candidates = buildList {
            if (local != null) {
                add(Candidate(RegionalEvidenceScope.SAME_HOUR, compatible.filter {
                    it.weekday == local.dayOfWeek.value && it.hour == local.hour
                }, 10))
                val adjacent = (-1L..1L).map { local.plusHours(it) }.map { it.dayOfWeek.value to it.hour }.toSet()
                add(Candidate(RegionalEvidenceScope.ADJACENT_HOURS, compatible.filter {
                    (it.weekday to it.hour) in adjacent
                }, 15))
                add(Candidate(RegionalEvidenceScope.SAME_HOUR_OTHER_DAYS, compatible.filter { it.hour == local.hour }, 20))
            }
            add(Candidate(RegionalEvidenceScope.REGION_BASELINE, compatible, 30))
        }
        val selected = candidates.firstOrNull {
            val completed = it.rows.sumOf(WaitingObservation::completed)
            val all = completed + it.rows.sumOf(WaitingObservation::interrupted)
            completed >= it.minimum && completed.toDouble() / all >= 0.7
        }
        val rows = selected?.rows ?: compatible
        val completed = rows.sumOf { it.completed }
        val interrupted = rows.sumOf { it.interrupted }
        val mean = if (completed > 0) rows.sumOf { it.completedMillis } / completed.toDouble() / 1000 else null
        val outlook = when {
            selected == null || mean == null -> WaitingOutlook.INSUFFICIENT
            mean <= 180 -> WaitingOutlook.SHORT
            mean > 600 -> WaitingOutlook.LONG
            else -> WaitingOutlook.MODERATE
        }
        return WaitingAssessment(outlook, selected?.scope ?: RegionalEvidenceScope.REGION_BASELINE,
            completed, interrupted, mean)
    }
}
