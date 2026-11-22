package io.github.pennymorant.offerpilot.ui.rules

import io.github.pennymorant.offerpilot.model.HourlyBasis
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.model.RuleCondition

enum class RuleField {
    MINIMUM, MILE, HOUR, MINIMUM_CONTRIBUTION,
    TARGET_CONTRIBUTION, TARGET_CONTRIBUTION_MILE, TARGET_CONTRIBUTION_HOUR, MINIMUM_SCORE,
    DISTANCE, TIME, PICKUP, COST, EXCLUDED, MERCHANTS, PREFERRED_ZONES, REGION_PERCENT, REGION_SAMPLES,
    MAX_SHOP_ITEMS, RADAR_EXTRA_SCORE,
}

data class RulesUiState(
    val minimum: String,
    val mile: String,
    val hour: String,
    val minimumContribution: String,
    val targetContribution: String,
    val targetContributionMile: String,
    val targetContributionHour: String,
    val minimumScore: String,
    val distance: String,
    val time: String,
    val pickup: String,
    val cost: String,
    val excluded: String,
    val platforms: Set<Platform>,
    val stacked: Boolean,
    val addon: Boolean,
    val allowPackage: Boolean = true,
    val allowShopAndPay: Boolean = true,
    val maxShopItems: String = "",
    val allowTripRadar: Boolean = true,
    val radarExtraScore: String = "5",
    val excludedMerchants: String = "",
    val preferredZones: String = "",
    val disabledConditions: Set<RuleCondition> = emptySet(),
    val scope: Platform? = null,
    val inheritShared: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<RuleField, String> = emptyMap(),
    val hourlyBasis: HourlyBasis = HourlyBasis.GROSS,
    val regionalEnabled: Boolean = false,
    val regionalGoodPercent: String = "50",
    val regionalSamples: String = "20",
)
