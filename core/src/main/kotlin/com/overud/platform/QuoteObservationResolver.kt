package com.overud.platform

import com.overud.model.*

/** Entire observations are selected; fields from different frames are never spliced together. */
data class QuoteObservation(val page: PlatformPage, val offer: Offer?, val sourceConflict: Boolean = false)

object QuoteObservationResolver {
    fun resolve(platform: Platform, nodes: String, ocr: String?, now: Long): QuoteObservation {
        val page = OfferParser.classify(platform, nodes)
        val nodeOffer = OfferParser.parse(platform, nodes, now)
        if (!page.allowsOcrFallback || ocr == null) return QuoteObservation(page, nodeOffer)
        val ocrPage = OfferParser.classify(platform, ocr, OfferSource.OCR)
        val ocrOffer = OfferParser.parse(platform, ocr, now, OfferSource.OCR)
        if (nodeOffer == null) return QuoteObservation(ocrPage, ocrOffer)
        if (ocrPage.type != PlatformPageType.UNKNOWN && !ocrPage.allowsOffer) return conflict(page, ocrPage)
        if (ocrOffer == null) return QuoteObservation(page, nodeOffer)
        if (disagrees(nodeOffer, ocrOffer)) return conflict(page, ocrPage)
        val amount = nodeOffer.amountCents
        val anchored = amount != null && amount == ocrOffer.amountCents && (
            sameLocation(nodeOffer.destination, ocrOffer.destination) || sameLocation(nodeOffer.merchant, ocrOffer.merchant) ||
                (nodeOffer.distanceMeters != null && nodeOffer.distanceMeters == ocrOffer.distanceMeters &&
                    nodeOffer.durationSeconds != null && nodeOffer.durationSeconds == ocrOffer.durationSeconds))
        return if (anchored && completeness(ocrOffer) > completeness(nodeOffer)) QuoteObservation(ocrPage, ocrOffer)
            else QuoteObservation(page, nodeOffer)
    }

    private fun disagrees(a: Offer, b: Offer): Boolean {
        fun conflict(x: Any?, y: Any?) = x != null && y != null && x != y
        return (a.serviceKind != OfferService.UNKNOWN && b.serviceKind != OfferService.UNKNOWN && a.serviceKind != b.serviceKind) ||
            conflict(normalize(a.pickupAddress), normalize(b.pickupAddress)) || conflict(a.includesTip, b.includesTip) ||
            conflict(a.amountCents, b.amountCents) || conflict(a.distanceMeters, b.distanceMeters) ||
            conflict(a.durationSeconds, b.durationSeconds) || a.currency != b.currency || a.isAddon != b.isAddon || a.isStacked != b.isStacked ||
            conflict(normalize(a.destination), normalize(b.destination)) || conflict(normalize(a.merchant), normalize(b.merchant))
    }

    private fun completeness(offer: Offer) = listOf(offer.amountCents != null,
        offer.hasVerifiedDistance(), offer.hasVerifiedDuration(), offer.destination != null).count { it }

    private fun normalize(value: String?) = value?.trim()?.lowercase(java.util.Locale.ROOT)
        ?.replace(Regex("\\s+"), " ")?.takeIf(String::isNotBlank)
    private fun sameLocation(a: String?, b: String?) = normalize(a)?.let { it == normalize(b) } == true
    private fun conflict(a: PlatformPage, b: PlatformPage) = QuoteObservation(
        a.copy(type = PlatformPageType.AMBIGUOUS, signals = a.signals + b.signals), null, true)
}
