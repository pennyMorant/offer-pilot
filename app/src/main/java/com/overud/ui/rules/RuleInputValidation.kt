package com.overud.ui.rules

import com.overud.model.RuleCondition
import java.math.BigDecimal
import java.math.RoundingMode

/** Input feedback only; RuleInputParser and the core policy still decide whether saving is valid. */
fun ruleInputErrors(input: RulesUiState): Map<RuleField, String> = buildMap {
    if (input.scope != null && input.inheritShared) return@buildMap
    fun money(field: RuleField, text: String, allowZero: Boolean = false) {
        val valid = runCatching {
            val value = text.toBigDecimal()
            value.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
            value <= BigDecimal("100000") && if (allowZero) value >= BigDecimal.ZERO else value > BigDecimal.ZERO
        }.getOrDefault(false)
        if (!valid) put(field, if (allowZero) "请输入 0–100000，最多两位小数" else "请输入大于 0 且不超过 100000 的金额，最多两位小数")
    }
    fun distance(field: RuleField, text: String) {
        val value = text.toDoubleOrNull()
        if (value == null || !value.isFinite() || value <= 0 || value > 1000 || value * 1609.344 < 0.5)
            put(field, "请输入有效英里数，大于 0 且不超过 1000")
    }
    fun active(condition: RuleCondition) = condition !in input.disabledConditions
    if (active(RuleCondition.MINIMUM_AMOUNT)) money(RuleField.MINIMUM, input.minimum)
    if (active(RuleCondition.MINIMUM_PER_MILE)) money(RuleField.MILE, input.mile)
    if (active(RuleCondition.MINIMUM_HOURLY)) money(RuleField.HOUR, input.hour)
    if (active(RuleCondition.MINIMUM_CONTRIBUTION)) {
        money(RuleField.MINIMUM_CONTRIBUTION, input.minimumContribution, true)
    }
    money(RuleField.TARGET_CONTRIBUTION, input.targetContribution)
    money(RuleField.TARGET_CONTRIBUTION_MILE, input.targetContributionMile)
    money(RuleField.TARGET_CONTRIBUTION_HOUR, input.targetContributionHour)
    if (input.minimumScore.toIntOrNull()?.let { it in 1..100 } != true) {
        put(RuleField.MINIMUM_SCORE, "请输入 1–100 的整数")
    }
    money(RuleField.COST, input.cost, true)
    if (active(RuleCondition.MAXIMUM_DISTANCE)) distance(RuleField.DISTANCE, input.distance)
    if (active(RuleCondition.MAXIMUM_PICKUP)) distance(RuleField.PICKUP, input.pickup)
    if (active(RuleCondition.MAXIMUM_DURATION) && input.time.toLongOrNull()?.let { it in 1L..1440L } != true)
        put(RuleField.TIME, "请输入 1–1440 的整数分钟")
    if (input.regionalEnabled) {
        if (input.regionalGoodPercent.toIntOrNull()?.let { it in 1..100 } != true) put(RuleField.REGION_PERCENT, "请输入 1–100 的整数")
        if (input.regionalSamples.toLongOrNull()?.let { it in 10L..100000L } != true) put(RuleField.REGION_SAMPLES, "请输入 10–100000 的整数")
    }
    if (input.allowShopAndPay && input.maxShopItems.isNotBlank() &&
        input.maxShopItems.trim().toIntOrNull()?.let { it in 1..200 } != true) {
        put(RuleField.MAX_SHOP_ITEMS, "请输入 1–200 的整数，或留空表示不限")
    }
    if (input.allowTripRadar && input.radarExtraScore.trim().toIntOrNull()?.let { it in 0..30 } != true) {
        put(RuleField.RADAR_EXTRA_SCORE, "请输入 0–30 的整数")
    }
}
