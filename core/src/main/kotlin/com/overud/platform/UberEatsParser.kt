package com.overud.platform

import com.overud.model.*

object UberEatsParser : TextOfferParser(Platform.UBER_EATS, UberEatsPages.rules) {
    override fun parse(text: String, now: Long, source: OfferSource): Offer? {
        val page = classify(text, source)
        if (!page.allowsOffer) return null
        val generic = super.parse(text, now, source)
        val kind = UberOfferCard.kind(text)
        if (generic?.isStacked == true) return generic.copy(serviceKind = kind, reliable = false)
        val card = if (page.type == PlatformPageType.OFFER) UberOfferCard.read(text) else null
        if (card == null) return generic?.let {
            if (kind == OfferService.UNKNOWN) it else it.copy(serviceKind = kind, reliable = false)
        }
        val parsed = super.parse(card.canonicalText, now, source) ?: return null
        val complete = card.destination != null && (parsed.merchant != null || card.pickup != null)
        return parsed.copy(
            id = fingerprint(Platform.UBER_EATS, "${parsed.id}|${card.kind}|${card.pickup}"),
            serviceKind = card.kind, pickupAddress = card.pickup, includesTip = true,
            reliable = parsed.reliable && complete, rawText = text,
        )
    }
}
