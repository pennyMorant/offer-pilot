package io.github.pennymorant.offerpilot.model

enum class WaitingOutlook { SHORT, MODERATE, LONG, INSUFFICIENT }

data class WaitingAssessment(
    val outlook: WaitingOutlook = WaitingOutlook.INSUFFICIENT,
    val scope: RegionalEvidenceScope = RegionalEvidenceScope.NONE,
    val completedCount: Long = 0,
    val interruptedCount: Long = 0,
    val completedMeanSeconds: Double? = null,
)
