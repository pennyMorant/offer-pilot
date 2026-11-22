package io.github.pennymorant.offerpilot.model

data class AcceptanceRules(
    val minimumCents: Long = 800,
    val minimumCentsPerMile: Long = 200,
    val minimumCentsPerHour: Long = 2500,
    val minimumContributionCents: Long = 500,
    val targetContributionCents: Long = 800,
    val targetContributionCentsPerMile: Long = 165,
    val targetContributionCentsPerHour: Long = 2000,
    val minimumValueScore: Int = 70,
    val maximumMeters: Long = 16093,
    val maximumSeconds: Long = 2700,
    val maximumPickupMeters: Long? = null,
    val costCentsPerMile: Long = 0,
    val allowStacked: Boolean = false,
    val allowAddon: Boolean = false,
    val allowPackage: Boolean = true,
    val allowShopAndPay: Boolean = true,
    val maximumShopItems: Int? = null,
    val allowTripRadar: Boolean = true,
    val radarExtraScore: Int = 5,
    val excludedDestinations: Set<String> = emptySet(),
    val excludedMerchants: Set<String> = emptySet(),
    val preferredZones: Set<String> = emptySet(),
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
            RuleCondition.MINIMUM_CONTRIBUTION,
        )
        return minimumCents > 0 && minimumCentsPerMile > 0 && minimumCentsPerHour > 0 &&
            minimumContributionCents >= 0 && targetContributionCents > 0 &&
            targetContributionCentsPerMile > 0 && targetContributionCentsPerHour > 0 &&
            minimumValueScore in 1..100 &&
            maximumMeters > 0 && maximumSeconds > 0 && costCentsPerMile >= 0 &&
            (maximumPickupMeters == null || maximumPickupMeters > 0) &&
            (maximumShopItems == null || maximumShopItems in 1..200) &&
            radarExtraScore in 0..30 &&
            incomeConditions.any(::enabled) && regionalRequirement.valid()
    }
}
