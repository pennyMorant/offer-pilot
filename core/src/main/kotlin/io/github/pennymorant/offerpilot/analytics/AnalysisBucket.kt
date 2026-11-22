package io.github.pennymorant.offerpilot.analytics

import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.model.OfferService
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AnalysisBucket(
    val platform: Platform,
    val region: String,
    val hourStart: Long,
    val zoneId: String,
    val localDate: String,
    val localHour: Int,
    val weekday: Int,
    val qualityVersion: Int = QualityEvaluator.VERSION,
    val serviceKind: OfferService = OfferService.UNKNOWN,
) {
    val id get() = "${platform.name}|$region|$hourStart|$zoneId|$qualityVersion|${serviceKind.name}"

    companion object {
        const val UNKNOWN_REGION = "UNKNOWN"

        fun at(platform: Platform, region: String?, time: Long, zone: ZoneId = ZoneId.systemDefault()): AnalysisBucket {
            val local = Instant.ofEpochMilli(time).atZone(zone)
            return AnalysisBucket(platform, region ?: UNKNOWN_REGION,
                local.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli(), zone.id,
                local.toLocalDate().toString(), local.hour, local.dayOfWeek.value)
        }
    }
}
