package com.overud.model

data class AcceptanceRules(
    val minimumCents: Long = 800,
    val minimumCentsPerMile: Long = 200,
    val minimumCentsPerHour: Long = 2500,
    val maximumMeters: Long = 16093,
    val maximumSeconds: Long = 2700,
    val maximumPickupMeters: Long? = null,
    val costCentsPerMile: Long = 35,
    val allowStacked: Boolean = false,
    val allowAddon: Boolean = false,
    val excludedDestinations: Set<String> = emptySet(),
    val excludedMerchants: Set<String> = emptySet(),
    val disabledConditions: Set<RuleCondition> = emptySet(),
    val hourlyBasis: HourlyBasis = HourlyBasis.GROSS,
    val regionalRequirement: RegionalRequirement = RegionalRequirement(),
) {
    fun enabled(condition: RuleCondition) = condition !in disabledConditions

    fun valid(): Boolean {
        val incomeConditions = setOf(
            RuleCondition.MINIMUM_AMOUNT,
            RuleCondition.MINIMUM_PER_MILE,
            RuleCondition.MINIMUM_HOURLY,
        )
        return minimumCents > 0 && minimumCentsPerMile > 0 && minimumCentsPerHour > 0 &&
            maximumMeters > 0 && maximumSeconds > 0 && costCentsPerMile >= 0 &&
            (maximumPickupMeters == null || maximumPickupMeters > 0) &&
            incomeConditions.any(::enabled) && regionalRequirement.valid()
    }
}
