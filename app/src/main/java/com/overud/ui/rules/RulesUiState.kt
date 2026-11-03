package com.overud.ui.rules

import com.overud.model.HourlyBasis
import com.overud.model.Platform
import com.overud.model.RuleCondition

enum class RuleField { MINIMUM, MILE, HOUR, DISTANCE, TIME, PICKUP, COST, EXCLUDED, MERCHANTS, REGION_PERCENT, REGION_SAMPLES }

data class RulesUiState(
    val minimum: String,
    val mile: String,
    val hour: String,
    val distance: String,
    val time: String,
    val pickup: String,
    val cost: String,
    val excluded: String,
    val platforms: Set<Platform>,
    val stacked: Boolean,
    val addon: Boolean,
    val excludedMerchants: String = "",
    val disabledConditions: Set<RuleCondition> = emptySet(),
    val scope: Platform? = null,
    val inheritShared: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val hourlyBasis: HourlyBasis = HourlyBasis.GROSS,
    val regionalEnabled: Boolean = false,
    val regionalGoodPercent: String = "50",
    val regionalSamples: String = "20",
)
