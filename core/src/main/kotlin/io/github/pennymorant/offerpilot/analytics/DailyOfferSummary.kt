package io.github.pennymorant.offerpilot.analytics

import io.github.pennymorant.offerpilot.model.OfferChannel
import io.github.pennymorant.offerpilot.model.OfferOutcome
import io.github.pennymorant.offerpilot.model.OfferService
import io.github.pennymorant.offerpilot.model.Platform
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Pure Kotlin item projection of persisted offer_history rows for daily analytics. */
data class LoggedOfferItem(
    val offerId: String,
    val platform: Platform,
    val observedAt: Long,
    val localDate: String,
    val localHour: Int,
    val serviceKind: OfferService,
    val channel: OfferChannel,
    val preferredTag: Boolean,
    val fakePreferred: Boolean,
    val itemCount: Int,
    val amountCents: Long,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val centsPerMile: Long,
    val grossCentsPerHour: Long,
    val valueScore: Int,
    val quality: OrderQuality,
    val eligible: Boolean,
    val accepted: Boolean = false,
    val outcome: OfferOutcome = if (accepted) OfferOutcome.ACCEPTED else OfferOutcome.PENDING,
    val merchant: String,
    val destinationArea: String,
)

data class DailyOfferSummary(
    val totalCount: Int = 0,
    val classifiedCount: Int = 0,
    val goodCount: Int = 0,
    val normalCount: Int = 0,
    val lowCount: Int = 0,
    val unknownCount: Int = 0,
    val goodPercent: Int = 0,
    val normalPercent: Int = 0,
    val lowPercent: Int = 0,
    val eligibleCount: Int = 0,
    val eligiblePercent: Int = 0,
    val acceptedCount: Int = 0,
    val declinedCount: Int = 0,
    val decidedCount: Int = 0,
    val acceptedPercent: Int = 0,
    val exclusiveAcceptedCount: Int = 0,
    val exclusiveDecidedCount: Int = 0,
    val exclusiveArPercent: Int = 0,
    val acceptedAmountCents: Long = 0,
    val radarCount: Int = 0,
    val exclusiveCount: Int = 0,
    val preferredCount: Int = 0,
    val fakePreferredCount: Int = 0,
    val shopCount: Int = 0,
    val avgAmountCents: Long? = null,
    val avgDistanceMeters: Long? = null,
    val avgCentsPerMile: Long? = null,
    val avgGrossCentsPerHour: Long? = null,
    val hourlyCounts: List<Int> = List(24) { 0 },
) {
    companion object {
        fun from(items: List<LoggedOfferItem>): DailyOfferSummary {
            if (items.isEmpty()) return DailyOfferSummary()
            val good = items.count { it.quality == OrderQuality.GOOD }
            val normal = items.count { it.quality == OrderQuality.NORMAL }
            val low = items.count { it.quality == OrderQuality.LOW }
            val unknown = items.count { it.quality == OrderQuality.UNKNOWN }
            val classified = good + normal + low
            fun pct(count: Int, denom: Int) = if (denom > 0) (count * 100.0 / denom).roundToInt() else 0
            val eligible = items.count { it.eligible }
            val acceptedItems = items.filter { it.outcome == OfferOutcome.ACCEPTED }
            val declinedCount = items.count { it.outcome == OfferOutcome.DECLINED }
            val decidedCount = acceptedItems.size + declinedCount
            val exclusiveDecided = items.filter {
                it.channel == OfferChannel.EXCLUSIVE &&
                    (it.outcome == OfferOutcome.ACCEPTED || it.outcome == OfferOutcome.DECLINED)
            }
            val exclusiveAccepted = exclusiveDecided.count { it.outcome == OfferOutcome.ACCEPTED }
            val amounts = items.map { it.amountCents }.filter { it > 0 }
            val distances = items.map { it.distanceMeters }.filter { it > 0 }
            val perMiles = items.map { it.centsPerMile }.filter { it > 0 }
            val perHours = items.map { it.grossCentsPerHour }.filter { it > 0 }
            val hours = IntArray(24)
            items.forEach { if (it.localHour in 0..23) hours[it.localHour]++ }
            return DailyOfferSummary(
                totalCount = items.size,
                classifiedCount = classified,
                goodCount = good,
                normalCount = normal,
                lowCount = low,
                unknownCount = unknown,
                goodPercent = pct(good, classified),
                normalPercent = pct(normal, classified),
                lowPercent = pct(low, classified),
                eligibleCount = eligible,
                eligiblePercent = pct(eligible, items.size),
                acceptedCount = acceptedItems.size,
                declinedCount = declinedCount,
                decidedCount = decidedCount,
                acceptedPercent = pct(acceptedItems.size, decidedCount),
                exclusiveAcceptedCount = exclusiveAccepted,
                exclusiveDecidedCount = exclusiveDecided.size,
                exclusiveArPercent = pct(exclusiveAccepted, exclusiveDecided.size),
                acceptedAmountCents = acceptedItems.sumOf { it.amountCents },
                radarCount = items.count { it.channel == OfferChannel.TRIP_RADAR },
                exclusiveCount = items.count { it.channel == OfferChannel.EXCLUSIVE },
                preferredCount = items.count { it.preferredTag },
                fakePreferredCount = items.count { it.fakePreferred },
                shopCount = items.count { it.serviceKind == OfferService.SHOP_AND_PAY },
                avgAmountCents = amounts.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
                avgDistanceMeters = distances.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
                avgCentsPerMile = perMiles.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
                avgGrossCentsPerHour = perHours.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
                hourlyCounts = hours.toList(),
            )
        }
    }
}
