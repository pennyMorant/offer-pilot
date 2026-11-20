package com.overud.platform

import com.overud.model.*

/** One page and source at a time; unknown scopes remain display-only for that metric. */
abstract class TextOfferParser(
    protected val platform: Platform,
    protected val pages: PlatformPageRules,
) : OfferParser {
    private val stackedRegex = Regex("\\b[2-9]\\s*(?:orders|deliveries|stops)\\b|\\([2-9]\\)|叠单", RegexOption.IGNORE_CASE)
    private val itemsRegex = Regex("\\b(\\d{1,3})\\s*(?:items?|units?|件)\\b", RegexOption.IGNORE_CASE)

    override fun classify(text: String, source: OfferSource) = PageClassifier.classify(platform, text, pages, source)

    override fun parse(text: String, now: Long, source: OfferSource): Offer? =
        parseInternal(cardText = text, fullText = text, amountHint = null, cardScoped = false, now = now, source = source)

    override fun parseRegions(regions: List<TextRegion>, now: Long, source: OfferSource): Offer? {
        val located = OfferCardLocator.locate(platform, regions)
        if (located.fullText.isBlank()) return null
        val scoped = located.amountRegion != null || located.actionRegion != null
        return parseInternal(
            cardText = located.cardText,
            fullText = located.fullText,
            amountHint = located.amountRegion?.text,
            cardScoped = scoped,
            now = now,
            source = source,
        )
    }

    protected fun parseInternal(
        cardText: String,
        fullText: String,
        amountHint: String?,
        cardScoped: Boolean,
        now: Long,
        source: OfferSource,
    ): Offer? {
        val page = classify(fullText, source)
        if (!page.allowsOffer) return null
        val amount = if (cardScoped) QuoteMoneyParser.parseCard(cardText, amountHint) else QuoteMoneyParser.parse(cardText)
        val addon = page.type == PlatformPageType.ADDON_OFFER
        val route = RouteMetricsParser.parse(cardText, addon, cardScoped)
        val locations = DeliveryLocationParser.parse(cardText)
        val totals = route.distanceVerified && route.durationVerified
        val incremental = addon && amount.incremental && totals
        val amountVerified = amount.cents != null && amount.currency == "USD"
        val items = itemsRegex.find(cardText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..500 }
        return Offer(
            id = fingerprint(platform, listOf(amount.cents, amount.currency, route.meters, route.seconds,
                locations.merchant, locations.destination, addon).joinToString("|")),
            platform = platform, observedAt = now,
            amountCents = amount.cents, distanceMeters = route.meters, durationSeconds = route.seconds,
            merchant = locations.merchant, destination = locations.destination, pickupMeters = route.pickupMeters,
            currency = amount.currency,
            isStacked = stackedRegex.containsMatchIn(cardText),
            isAddon = addon,
            reliable = source == OfferSource.UI_NODES && amountVerified && totals && (!addon || incremental),
            rawText = fullText,
            evidence = QuoteEvidence(amountVerified, totals, source, incremental,
                route.distanceVerified, route.durationVerified),
            itemCount = items,
        )
    }
}
