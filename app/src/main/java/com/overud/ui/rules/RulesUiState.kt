package com.overud.ui.rules

import com.overud.model.Platform

enum class RuleField { MINIMUM, MILE, HOUR, DISTANCE, TIME, PICKUP, COST, EXCLUDED }

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
    val error: String? = null,
)

data class RulePreviewState(
    val text: String = "Accept\n$12.00\nTotal 5 mi 25 min\nMerchant: Restaurant\nDestination: Downtown",
    val platform: Platform = Platform.UBER_EATS,
    val explanation: String = "",
)
