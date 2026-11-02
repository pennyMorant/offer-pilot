package com.overud.model

enum class DestinationResolution { MISSING, RESOLVING, UNAVAILABLE, AMBIGUOUS, TOO_BROAD, RESOLVED }
enum class DestinationPrecision { COORDINATE, ADDRESS, STREET }
enum class RegionalOutlook { GOOD, MIXED, WEAK, INSUFFICIENT }
enum class RegionalEvidenceScope { SAME_HOUR, ADJACENT_HOURS, SAME_HOUR_OTHER_DAYS, NEIGHBORHOOD, REGION_BASELINE, NONE }

data class ResolvedDestination(
    val status: DestinationResolution,
    val regionKey: String? = null,
    val precision: DestinationPrecision? = null,
)

data class DestinationAssessment(
    val resolution: DestinationResolution = DestinationResolution.MISSING,
    val regionKey: String? = null,
    val precision: DestinationPrecision? = null,
    val arrivalAt: Long? = null,
    val outlook: RegionalOutlook = RegionalOutlook.INSUFFICIENT,
    val evidenceScope: RegionalEvidenceScope = RegionalEvidenceScope.NONE,
    val observedCount: Long = 0,
    val classifiedCount: Long = 0,
    val goodRate: Double? = null,
)
