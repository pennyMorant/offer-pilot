package com.overud.ui.dashboard

import com.overud.model.*
import com.overud.ui.components.money
import com.overud.valuation.ValueEngine
import java.util.Locale

data class PlatformPolicySummary(val label: String, val details: String)

fun policySummaries(policy: Policy): List<PlatformPolicySummary> = Platform.entries
    .filter { it in policy.enabledPlatforms }
    .map { platform ->
        val rules = policy.effective(platform).sharedRules()
        val details = buildList {
            if (rules.enabled(RuleCondition.MINIMUM_AMOUNT)) add("报价 ≥ ${money(rules.minimumCents)}")
            if (rules.enabled(RuleCondition.MINIMUM_PER_MILE)) add("≥ ${money(rules.minimumCentsPerMile)}/mi")
            if (rules.enabled(RuleCondition.MINIMUM_HOURLY)) add("毛时薪 ≥ ${money(rules.minimumCentsPerHour)}/h")
            if (rules.enabled(RuleCondition.MAXIMUM_DISTANCE)) add("≤ ${miles(rules.maximumMeters)} mi")
            if (rules.enabled(RuleCondition.MAXIMUM_DURATION)) add("≤ ${rules.maximumSeconds / 60} min")
            val pickup = rules.maximumPickupMeters
            if (rules.enabled(RuleCondition.MAXIMUM_PICKUP) && pickup != null) add("取餐 ≤ ${miles(pickup)} mi")
            if (rules.excludedDestinations.isNotEmpty()) add("排除 ${rules.excludedDestinations.size} 项目的地")
            if (rules.excludedMerchants.isNotEmpty()) add("排除 ${rules.excludedMerchants.size} 项商家")
        }.joinToString(" · ")
        val source = if (platform in policy.platformOverrides) "独立要求" else "公共要求"
        PlatformPolicySummary("${platform.label} · $source", details)
    }

private fun miles(meters: Long) = "%.1f".format(Locale.US, meters / ValueEngine.METERS_PER_MILE)
