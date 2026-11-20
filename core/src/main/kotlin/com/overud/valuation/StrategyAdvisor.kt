package com.overud.valuation

import com.overud.model.*

data class StrategyAdvice(
    val tags: List<String> = emptyList(),
    val headline: String? = null,
    val fakePreferred: Boolean = false,
)

/**
 * Interprets platform dispatch mechanics (Trip Radar, Preferred Deliveries, Shop & Pay)
 * without altering core hard-rule eligibility or historical quality v2 standards.
 */
object StrategyAdvisor {
    private const val PICKING_SECONDS_PER_ITEM = 45

    fun advise(offer: Offer, decision: Decision?): StrategyAdvice {
        val tags = mutableListOf<String>()
        val perMile = decision?.valuation?.centsPerMile
        val score = decision?.valuation?.score
        val fakePreferred = offer.preferredTag && (
            decision?.eligible == false ||
                (perMile != null && perMile < 150.0) ||
                (score != null && score < 70)
            )

        when {
            fakePreferred -> tags += "⚠ 优选标签低效（假好单）"
            offer.preferredTag && decision?.eligible == true -> tags += "★ 优选真好单"
            offer.preferredTag -> tags += "优选标签"
        }

        when (offer.channel) {
            OfferChannel.TRIP_RADAR -> tags += "雷达单（拒单不扣AR）"
            OfferChannel.EXCLUSIVE -> tags += "专属单（计入AR）"
            OfferChannel.UNKNOWN -> Unit
        }

        if (offer.serviceKind == OfferService.SHOP_AND_PAY) {
            val items = offer.itemCount
            if (items != null) {
                val pickMinutes = ((items * PICKING_SECONDS_PER_ITEM) + 59) / 60
                tags += "商超 ${items}件（估拣货~${pickMinutes}分）"
            } else {
                tags += "商超采购"
            }
        }

        val headline = when {
            fakePreferred -> "优选标签但每英里/评分偏低，警惕高总额长途假好单"
            offer.channel == OfferChannel.TRIP_RADAR && decision?.eligible == true -> "雷达好单，抢单不伤接单率"
            offer.channel == OfferChannel.TRIP_RADAR -> "雷达单拒绝不扣 AR，不合算直接放过"
            else -> tags.firstOrNull()
        }
        return StrategyAdvice(tags, headline, fakePreferred)
    }
}
