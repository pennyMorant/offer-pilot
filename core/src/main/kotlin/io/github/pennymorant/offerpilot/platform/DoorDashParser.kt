package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.*

object DoorDashParser : TextOfferParser(Platform.DOORDASH, DoorDashPages.rules) {
    override fun parse(text: String, now: Long, source: OfferSource): Offer? {
        val cardScoped = text.lines().any { it.trim().equals("Accept", true) || it.trim().equals("Decline", true) }
        val base = parseInternal(text, text, null, cardScoped, now, source) ?: return null
        return enrich(base, DoorDashCard.read(text))
    }

    override fun parseRegions(regions: List<TextRegion>, now: Long, source: OfferSource): Offer? {
        val located = OfferCardLocator.locate(Platform.DOORDASH, regions)
        if (located.fullText.isBlank()) return null
        val scoped = located.amountRegion != null || located.actionRegion != null
        val base = parseInternal(
            cardText = located.cardText,
            fullText = located.fullText,
            amountHint = located.amountRegion?.text,
            cardScoped = scoped,
            now = now,
            source = source,
        ) ?: return null
        return enrich(base, DoorDashCard.read(located.cardText))
    }

    private fun enrich(base: Offer, card: DoorDashCardData): Offer {
        val merchant = base.merchant ?: card.merchant
        val destination = base.destination ?: card.destination
        val kind = if (card.kind != OfferService.UNKNOWN) card.kind else base.serviceKind
        return base.copy(
            id = fingerprint(Platform.DOORDASH, listOf(base.amountCents, base.currency, base.distanceMeters,
                base.durationSeconds, merchant, destination, kind, base.isAddon).joinToString("|")),
            merchant = merchant,
            destination = destination,
            serviceKind = kind,
            includesTip = card.includesTip ?: base.includesTip,
            channel = card.channel,
            preferredTag = card.preferredTag,
            itemCount = card.itemCount ?: base.itemCount,
        )
    }
}
