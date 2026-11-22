package io.github.pennymorant.offerpilot.model

enum class OfferSource { UI_NODES, OCR }

data class QuoteEvidence(
    val amountVerified: Boolean,
    val totalsVerified: Boolean,
    val source: OfferSource = OfferSource.UI_NODES,
    val incrementalRouteVerified: Boolean = false,
    val distanceVerified: Boolean = false,
    val durationVerified: Boolean = false,
)

fun Offer.hasVerifiedDistance(): Boolean =
    (reliable || evidence?.totalsVerified == true || evidence?.distanceVerified == true) &&
        (!isAddon || evidence?.incrementalRouteVerified == true)

fun Offer.hasVerifiedDuration(): Boolean =
    (reliable || evidence?.totalsVerified == true || evidence?.durationVerified == true) &&
        (!isAddon || evidence?.incrementalRouteVerified == true)
