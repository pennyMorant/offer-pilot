package com.overud.analytics

import com.overud.model.OfferChannel
import com.overud.model.OfferService
import com.overud.model.Platform
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
