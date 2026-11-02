package com.overud.model

enum class OfferSource { UI_NODES, OCR }

data class QuoteEvidence(
    val amountVerified: Boolean,
    val totalsVerified: Boolean,
    val source: OfferSource = OfferSource.UI_NODES,
    val incrementalRouteVerified: Boolean = false,
)
