package com.overud.platform

import com.overud.model.*

object UberEatsParser : TextOfferParser(Platform.UBER_EATS, UberEatsPages.rules) {
    override fun parse(text: String, now: Long, source: OfferSource): Offer? {
        val card = UberOfferCard.read(text)
        val cardText = card?.cardText ?: text
        val base = parseInternal(cardText, text, null, card != null, now, source) ?: return null
        return enrich(base, card)
    }

    override fun parseRegions(regions: List<TextRegion>, now: Long, source: OfferSource): Offer? {
        val located = OfferCardLocator.locate(Platform.UBER_EATS, regions)
        if (located.fullText.isBlank()) return null
        val card = UberOfferCard.read(located.cardText) ?: UberOfferCard.read(located.fullText)
        val scoped = located.amountRegion != null || located.actionRegion != null || card != null
        val base = parseInternal(
            cardText = card?.cardText ?: located.cardText,
            fullText = located.fullText,
            amountHint = located.amountRegion?.text,
            cardScoped = scoped,
            now = now,
            source = source,
        ) ?: return null
        return enrich(base, card)
    }

    private fun enrich(base: Offer, card: UberCard?): Offer {
        if (card == null) return base
        val merchant = base.merchant ?: card.merchant
        val pickup = base.pickupAddress ?: card.pickup
        val destination = base.destination ?: card.destination
        val kind = if (card.kind != OfferService.UNKNOWN) card.kind else base.serviceKind
        return base.copy(
            id = fingerprint(Platform.UBER_EATS, listOf(base.amountCents, base.currency, base.distanceMeters,
                base.durationSeconds, merchant, pickup, destination, kind, base.isAddon).joinToString("|")),
            merchant = merchant,
            pickupAddress = pickup,
            destination = destination,
            serviceKind = kind,
            includesTip = card.includesTip ?: base.includesTip,
            channel = card.channel,
            preferredTag = card.preferredTag,
            itemCount = card.itemCount ?: base.itemCount,
        )
    }
}
