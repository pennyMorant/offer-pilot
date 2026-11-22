package io.github.pennymorant.offerpilot.model

/** Transient context binding; never persisted with an address or individual quote. */
data class DestinationEvidence(
    val token: String,
    val platform: Platform,
    val destination: String,
    val durationSeconds: Long?,
    val observedMinute: Long,
    val calculatedAt: Long,
    val historyRevision: Long,
    val qualityVersion: Int,
    val assessment: DestinationAssessment,
)
