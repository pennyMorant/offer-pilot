package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.*
import java.util.Locale

/** Explicit anchors are required; temporal proximity alone does not identify an order. */
object AcceptanceConfirmationPolicy {
    const val WINDOW_MILLIS = 15_000L

    fun matchesButton(platform: Platform, label: String?): Boolean {
        val labels = when (platform) {
            Platform.UBER_EATS -> UberEatsPages.rules.confirmation
            Platform.DOORDASH -> DoorDashPages.rules.confirmation
        }
        return label != null && normalize(label) in labels
    }

    fun matchesOffer(offer: Offer, text: String): Boolean {
        if (offer.isAddon || offer.isStacked || offer.currency != "USD") return false
        val page = OfferParser.classify(offer.platform, text)
        if (page.type != PlatformPageType.ACCEPT_CONFIRMATION) return false
        if (PageSignal.ADDON in page.signals || Regex("\\b[2-9] (orders|deliveries)\\b|叠单",
                RegexOption.IGNORE_CASE).containsMatchIn(text)) return false
        val money = QuoteMoneyParser.parse(text)
        if (money.cents == null || money.cents != offer.amountCents || money.currency != offer.currency) return false
        val locations = DeliveryLocationParser.parse(text)
        val originMatches = if (offer.serviceKind == OfferService.PACKAGE)
            same(offer.pickupAddress, locations.pickup) else same(offer.merchant, locations.merchant)
        if (!originMatches || !same(offer.destination, locations.destination)) return false
        // Require the same explicitly scoped route as well; changed or missing terms need manual review.
        val route = RouteMetricsParser.parse(text, false)
        if (route.pickupMeters != null && route.pickupMeters != offer.pickupMeters) return false
        val originalPickup = offer.pickupAddress ?: DeliveryLocationParser.parse(offer.rawText).pickup
        if (locations.pickup != null && !same(originalPickup, locations.pickup)) return false
        return route.distanceVerified && route.durationVerified &&
            route.meters == offer.distanceMeters && route.seconds == offer.durationSeconds
    }

    private fun same(expected: String?, actual: String?) =
        !expected.isNullOrBlank() && !actual.isNullOrBlank() && normalize(expected) == normalize(actual)

    private fun normalize(value: String) = value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
