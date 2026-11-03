package com.overud.ui.rules

import com.overud.model.RuleCondition
import com.overud.model.HourlyBasis

fun RulesUiState.incomeSummary() = buildList {
    if (RuleCondition.MINIMUM_AMOUNT !in disabledConditions) add("最低 $$minimum")
    if (RuleCondition.MINIMUM_PER_MILE !in disabledConditions) add("$$mile/mi")
    if (RuleCondition.MINIMUM_HOURLY !in disabledConditions) {
        val basis = if (hourlyBasis == HourlyBasis.GROSS) "毛时薪" else "成本后时薪"
        add("$basis $$hour/h")
    }
}.joinToString(" · ").ifBlank { "未启用最低收益条件" }

fun RulesUiState.routeSummary() = buildList {
    if (RuleCondition.MAXIMUM_DISTANCE !in disabledConditions) add("最多 $distance mi")
    if (RuleCondition.MAXIMUM_DURATION !in disabledConditions) add("最多 $time min")
    if (RuleCondition.MAXIMUM_PICKUP !in disabledConditions) add("取餐 $pickup mi 内")
}.joinToString(" · ").ifBlank { "未启用距离与时间限制" }
