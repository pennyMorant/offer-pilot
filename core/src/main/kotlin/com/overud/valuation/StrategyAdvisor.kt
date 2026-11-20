package com.overud.valuation

import com.overud.model.*

data class StrategyAdvice(
    val tags: List<String> = emptyList(),
    val headline: String? = null,
    val fakePreferred: Boolean = false,
    val highTipZone: Boolean = false,
)

/**
 * Interprets platform dispatch mechanics (Trip Radar, Preferred Deliveries, Shop & Pay)
 * without altering core hard-rule eligibility or historical quality v2 standards.
 */
object StrategyAdvisor {
    private const val PICKING_SECONDS_PER_ITEM = 45
    private val laHighTipZones = listOf(
        "santa monica", "beverly hills", "west hollywood", "pasadena", "arcadia",
        "irvine", "century city", "brentwood", "manhattan beach", "san marino",
        "culver city", "marina del rey", "playa vista", "south pasadena",
    )
    private val highTicketGrocers = listOf(
        "erewhon", "gelson", "sprouts", "total wine", "whole foods", "bristol farms", "pavilions",
    )

    fun advise(offer: Offer, decision: Decision?): StrategyAdvice {
        val tags = mutableListOf<String>()
        val perMile = decision?.valuation?.centsPerMile
        val score = decision?.valuation?.score
        val fakePreferred = offer.preferredTag && (
            decision?.eligible == false ||
                (perMile != null && perMile < 150.0) ||
                (score != null && score < 70)
            )
        val destLower = offer.destination?.lowercase(java.util.Locale.ROOT).orEmpty()
        val merchantLower = offer.merchant?.lowercase(java.util.Locale.ROOT).orEmpty()
        val highTipZone = destLower.isNotBlank() && laHighTipZones.any { it in destLower }

        when {
            fakePreferred -> tags += "⚠ 优选标签低效（假好单）"
            offer.preferredTag && decision?.eligible == true -> tags += "★ 优选真好单"
            offer.preferredTag -> tags += "优选标签"
        }

        if (highTipZone) tags += "LA 优选商圈"

        when (offer.channel) {
            OfferChannel.TRIP_RADAR -> tags += "雷达单（拒单不扣AR）"
            OfferChannel.EXCLUSIVE -> tags += "专属单（计入AR）"
            OfferChannel.UNKNOWN -> Unit
        }

        if (offer.serviceKind == OfferService.SHOP_AND_PAY) {
            val premium = merchantLower.isNotBlank() && highTicketGrocers.any { it in merchantLower }
            val prefix = if (premium) "高客单商超" else "商超"
            val items = offer.itemCount
            if (items != null) {
                val pickMinutes = ((items * PICKING_SECONDS_PER_ITEM) + 59) / 60
                tags += "$prefix ${items}件（估拣货~${pickMinutes}分）"
            } else {
                tags += "${prefix}采购"
            }
        }

        val headline = when {
            fakePreferred -> "优选标签但每英里/评分偏低，警惕高总额长途假好单"
            offer.channel == OfferChannel.TRIP_RADAR && decision?.eligible == true -> "雷达好单，抢单不伤接单率"
            offer.channel == OfferChannel.TRIP_RADAR -> "雷达单拒绝不扣 AR，不合算直接放过"
            else -> tags.firstOrNull()
        }
        return StrategyAdvice(tags, headline, fakePreferred, highTipZone)
    }
}
