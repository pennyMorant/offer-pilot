package io.github.pennymorant.offerpilot.model

enum class OfferOutcome {
    PENDING,
    ACCEPTED,
    DECLINED;

    companion object {
        fun fromStored(value: String?, acceptedFallback: Boolean): OfferOutcome =
            entries.find { it.name == value } ?: if (acceptedFallback) ACCEPTED else PENDING
    }
}
