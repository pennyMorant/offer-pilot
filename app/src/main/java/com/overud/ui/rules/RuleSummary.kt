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
    if (RuleCondition.MINIMUM_CONTRIBUTION !in disabledConditions) add("单笔贡献 $$minimumContribution")
}.joinToString(" · ").ifBlank { "未启用最低收益条件" }

fun RulesUiState.routeSummary() = buildList {
    if (RuleCondition.MAXIMUM_DISTANCE !in disabledConditions) add("最多 $distance mi")
    if (RuleCondition.MAXIMUM_DURATION !in disabledConditions) add("最多 $time min")
    if (RuleCondition.MAXIMUM_PICKUP !in disabledConditions) add("取餐 $pickup mi 内")
}.joinToString(" · ").ifBlank { "未启用距离与时间限制" }

fun RulesUiState.scoreSummary() =
    "最低 $minimumScore 分 · 目标 $$targetContributionHour/h · $$targetContributionMile/mi · 单笔 $$targetContribution"

fun RulesUiState.preferencesSummary() = buildList {
    add(if (allowTripRadar) "雷达 +${radarExtraScore.ifBlank { "0" }}分" else "禁雷达")
    add(if (allowShopAndPay) "商超${maxShopItems.takeIf(String::isNotBlank)?.let { " ≤${it}件" }.orEmpty()}" else "禁商超")
    if (!allowPackage) add("禁包裹")
    if (stacked) add("含叠单")
    if (addon) add("含追加")
    val zones = preferredZones.split(',', '，').map(String::trim).count(String::isNotBlank)
    if (zones > 0) add("优选商圈 $zones 项")
}.joinToString(" · ")
