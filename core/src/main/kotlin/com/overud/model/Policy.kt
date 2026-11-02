package com.overud.model

data class Policy(
    val version: Long = 1,
    val enabledPlatforms: Set<Platform> = Platform.entries.toSet(),
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
    val platformOverrides: Map<Platform, AcceptanceRules> = emptyMap(),
    val hourlyBasis: HourlyBasis = HourlyBasis.GROSS,
    val regionalRequirement: RegionalRequirement = RegionalRequirement(),
    val prop22: Prop22Policy = Prop22Policy(),
) {
    fun valid() = enabledPlatforms.isNotEmpty() && sharedRules().valid() &&
        platformOverrides.values.all(AcceptanceRules::valid)

    fun sharedRules() = AcceptanceRules(
        minimumCents = minimumCents,
        minimumCentsPerMile = minimumCentsPerMile,
        minimumCentsPerHour = minimumCentsPerHour,
        maximumMeters = maximumMeters,
        maximumSeconds = maximumSeconds,
        maximumPickupMeters = maximumPickupMeters,
        costCentsPerMile = costCentsPerMile,
        hourlyBasis = hourlyBasis,
        regionalRequirement = regionalRequirement,
        allowStacked = allowStacked,
        allowAddon = allowAddon,
        excludedDestinations = excludedDestinations,
        excludedMerchants = excludedMerchants,
        disabledConditions = disabledConditions,
    )

    fun effective(platform: Platform): Policy =
        withRules(platformOverrides[platform] ?: sharedRules()).copy(platformOverrides = emptyMap())

    fun withRules(rules: AcceptanceRules, platform: Platform? = null): Policy {
        if (platform != null) return copy(platformOverrides = platformOverrides + (platform to rules))
        return copy(
            minimumCents = rules.minimumCents,
            minimumCentsPerMile = rules.minimumCentsPerMile,
            minimumCentsPerHour = rules.minimumCentsPerHour,
            maximumMeters = rules.maximumMeters,
            maximumSeconds = rules.maximumSeconds,
            maximumPickupMeters = rules.maximumPickupMeters,
            costCentsPerMile = rules.costCentsPerMile,
            hourlyBasis = rules.hourlyBasis,
            regionalRequirement = rules.regionalRequirement,
            allowStacked = rules.allowStacked,
            allowAddon = rules.allowAddon,
            excludedDestinations = rules.excludedDestinations,
            excludedMerchants = rules.excludedMerchants,
            disabledConditions = rules.disabledConditions,
        )
    }
}
