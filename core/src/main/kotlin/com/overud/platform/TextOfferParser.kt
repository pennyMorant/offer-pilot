package com.overud.platform

import com.overud.model.*

/** One page and source at a time; unknown scopes remain display-only for that metric. */
abstract class TextOfferParser(
    private val platform: Platform,
    private val pages: PlatformPageRules,
) : OfferParser {
    override fun classify(text: String, source: OfferSource) = PageClassifier.classify(platform, text, pages, source)

    override fun parse(text: String, now: Long, source: OfferSource): Offer? {
        val page = classify(text, source)
        if (!page.allowsOffer) return null
        val amount = QuoteMoneyParser.parse(text)
        val addon = page.type == PlatformPageType.ADDON_OFFER
        val route = RouteMetricsParser.parse(text, addon)
        val locations = DeliveryLocationParser.parse(text)
        val totals = route.distanceVerified && route.durationVerified
        val incremental = addon && amount.incremental && totals
        val amountVerified = amount.cents != null && amount.currency == "USD"
        return Offer(
            id = fingerprint(platform, listOf(amount.cents, route.meters, route.seconds,
                locations.merchant, locations.destination, addon).joinToString("|")),
            platform = platform, observedAt = now,
            amountCents = amount.cents, distanceMeters = route.meters, durationSeconds = route.seconds,
            merchant = locations.merchant, destination = locations.destination, pickupMeters = route.pickupMeters,
            currency = amount.currency,
            isStacked = Regex("\\b[2-9] (orders|deliveries)\\b|叠单", RegexOption.IGNORE_CASE).containsMatchIn(text),
            isAddon = addon,
            reliable = source == OfferSource.UI_NODES && amountVerified && totals && (!addon || incremental),
            rawText = text,
            evidence = QuoteEvidence(amountVerified, totals, source, incremental,
                route.distanceVerified, route.durationVerified),
        )
    }
}
