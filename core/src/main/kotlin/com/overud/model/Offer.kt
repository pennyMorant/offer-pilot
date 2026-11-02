package com.overud.model

data class Offer(
    val id: String,
    val platform: Platform,
    val observedAt: Long,
    val amountCents: Long?,
    val distanceMeters: Long?,
    val durationSeconds: Long?,
    val merchant: String? = null,
    val destination: String? = null,
    val pickupMeters: Long? = null,
    val currency: String = "USD",
    val isStacked: Boolean = false,
    val isAddon: Boolean = false,
    val reliable: Boolean = false,
    val rawText: String = "",
    val evidence: QuoteEvidence? = null,
    val firstObservedAt: Long = observedAt,
)
