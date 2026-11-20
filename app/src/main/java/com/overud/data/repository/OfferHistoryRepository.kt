package com.overud.data.repository

import com.overud.analytics.DailyOfferSummary
import com.overud.analytics.LoggedOfferItem
import com.overud.analytics.OrderQuality
import com.overud.analytics.QualityEvaluator
import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.OfferHistoryRecord
import com.overud.model.*
import com.overud.valuation.StrategyAdvisor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

class OfferHistoryRepository(private val database: OverudDatabase) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private var prunedAt = 0L
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()

    fun record(offer: Offer, decision: Decision?, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (offer.currency != "USD") return false
        val amount = offer.amountCents?.takeIf { it > 0 } ?: return false
        val local = Instant.ofEpochMilli(offer.firstObservedAt).atZone(zone)
        val sample = QualityEvaluator.extract(offer)
        val advice = StrategyAdvisor.advise(offer, decision)
        val valuation = decision?.valuation
        val row = OfferHistoryRecord().apply {
            offerId = offer.id
            platform = offer.platform.name
            observedAt = offer.firstObservedAt
            localDate = local.toLocalDate().format(dateFormatter)
            localHour = local.hour
            serviceKind = offer.serviceKind.name
            channel = offer.channel.name
            preferredTag = offer.preferredTag
            fakePreferred = advice.fakePreferred
            itemCount = offer.itemCount ?: 0
            amountCents = amount
            distanceMeters = offer.distanceMeters?.takeIf { offer.hasVerifiedDistance() } ?: 0L
            durationSeconds = offer.durationSeconds?.takeIf { offer.hasVerifiedDuration() } ?: 0L
            centsPerMile = valuation?.centsPerMile?.roundToLong() ?: 0L
            grossCentsPerHour = valuation?.grossCentsPerHour?.roundToLong() ?: 0L
            valueScore = valuation?.score ?: -1
            quality = sample.quality.name
            eligible = decision?.eligible == true
            merchant = offer.merchant.orEmpty().trim().take(60)
            destinationArea = offer.destination.orEmpty().trim().take(60)
        }
        database.offerHistory().upsert(row)
        mutableRevision.value += 1
        return true
    }

    fun itemsByDateRange(startDate: String, endDate: String): List<LoggedOfferItem> =
        database.offerHistory().byDateRange(startDate, endDate).map(::toItem)

    fun recentDates(): List<String> = database.offerHistory().recentDates()

    fun prune(now: Long = System.currentTimeMillis()) {
        if (now - prunedAt < 12 * 3600_000L) return
        prunedAt = now
        database.offerHistory().pruneBefore(now - 90L * 86_400_000L)
        mutableRevision.value += 1
    }

    fun clearAll() {
        database.offerHistory().clearAll()
        mutableRevision.value += 1
    }

    private fun toItem(row: OfferHistoryRecord) = LoggedOfferItem(
        offerId = row.offerId,
        platform = Platform.entries.find { it.name == row.platform } ?: Platform.UBER_EATS,
        observedAt = row.observedAt,
        localDate = row.localDate,
        localHour = row.localHour,
        serviceKind = OfferService.entries.find { it.name == row.serviceKind } ?: OfferService.UNKNOWN,
        channel = OfferChannel.entries.find { it.name == row.channel } ?: OfferChannel.UNKNOWN,
        preferredTag = row.preferredTag,
        fakePreferred = row.fakePreferred,
        itemCount = row.itemCount,
        amountCents = row.amountCents,
        distanceMeters = row.distanceMeters,
        durationSeconds = row.durationSeconds,
        centsPerMile = row.centsPerMile,
        grossCentsPerHour = row.grossCentsPerHour,
        valueScore = row.valueScore,
        quality = OrderQuality.entries.find { it.name == row.quality } ?: OrderQuality.UNKNOWN,
        eligible = row.eligible,
        merchant = row.merchant,
        destinationArea = row.destinationArea,
    )
}
