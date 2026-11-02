package com.overud.model

enum class PlatformPageType {
    UNKNOWN, OFFER, ADDON_OFFER, ACCEPT_CONFIRMATION, ACCEPTED, PICKUP, DELIVERY,
    COMPLETED, CANCELLED, OFFLINE, HISTORY, AMBIGUOUS,
}

enum class PageSignal {
    QUOTE_ACTION, PRICE, ADDON, ACCEPT_CONFIRMATION, ACCEPTED, PICKUP, DELIVERY,
    COMPLETED, CANCELLED, OFFLINE, HISTORY,
}

data class PlatformPage(
    val platform: Platform,
    val type: PlatformPageType,
    val source: OfferSource = OfferSource.UI_NODES,
    val signals: Set<PageSignal> = emptySet(),
) {
    val allowsOffer: Boolean get() = type == PlatformPageType.OFFER || type == PlatformPageType.ADDON_OFFER
    val allowsOcrFallback: Boolean get() = allowsOffer || type == PlatformPageType.UNKNOWN
}
