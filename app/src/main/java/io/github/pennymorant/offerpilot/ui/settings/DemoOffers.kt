package io.github.pennymorant.offerpilot.ui.settings

import io.github.pennymorant.offerpilot.model.*

internal enum class DemoTier(val buttonLabel: String) {
    EXCELLENT("★ 必接"),
    GOOD("✓ 建议接"),
    BORDERLINE("△ 酌情"),
    SKIP("✕ 跳过"),
}

internal fun buildDemoOffer(tier: DemoTier, now: Long): Offer = when (tier) {
    DemoTier.EXCELLENT -> Offer(
        id = "demo-excellent-$now",
        platform = Platform.UBER_EATS,
        observedAt = now,
        amountCents = 1850,
        distanceMeters = 5150, // ~3.2 mi
        durationSeconds = 1200, // 20 min
        merchant = "Din Tai Fung (Arcadia)",
        destination = "Santa Anita Ave, Arcadia",
        reliable = true,
        evidence = verifiedEvidence(),
        serviceKind = OfferService.DELIVERY,
        includesTip = true,
        channel = OfferChannel.EXCLUSIVE,
    )

    DemoTier.GOOD -> Offer(
        id = "demo-good-$now",
        platform = Platform.DOORDASH,
        observedAt = now,
        amountCents = 830,
        distanceMeters = 6598, // ~4.1 mi
        durationSeconds = 1180, // ~20 min
        merchant = "Sweetfin (Pasadena)",
        destination = "Lake Ave, Pasadena",
        reliable = true,
        evidence = verifiedEvidence(),
        serviceKind = OfferService.DELIVERY,
        includesTip = true,
        channel = OfferChannel.EXCLUSIVE,
    )

    DemoTier.BORDERLINE -> Offer(
        id = "demo-borderline-$now",
        platform = Platform.UBER_EATS,
        observedAt = now,
        amountCents = 750,
        distanceMeters = 8047, // ~5.0 mi
        durationSeconds = 1560, // 26 min
        merchant = "Chipotle (Alhambra)",
        destination = "Main St, Alhambra",
        reliable = true,
        evidence = verifiedEvidence(),
        serviceKind = OfferService.DELIVERY,
        includesTip = true,
        channel = OfferChannel.TRIP_RADAR,
    )

    DemoTier.SKIP -> Offer(
        id = "demo-skip-$now",
        platform = Platform.UBER_EATS,
        observedAt = now,
        amountCents = 520,
        distanceMeters = 18507, // ~11.5 mi
        durationSeconds = 2160, // 36 min
        merchant = "Prime Pizza (DTLA)",
        destination = "Woodland Hills, Los Angeles",
        reliable = true,
        evidence = verifiedEvidence(),
        serviceKind = OfferService.DELIVERY,
        includesTip = true,
        channel = OfferChannel.EXCLUSIVE,
        preferredTag = true,
    )
}

private fun verifiedEvidence() = QuoteEvidence(
    amountVerified = true,
    totalsVerified = true,
    source = OfferSource.UI_NODES,
    distanceVerified = true,
    durationVerified = true,
)
