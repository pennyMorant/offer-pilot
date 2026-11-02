package com.overud.valuation

import com.overud.model.*

object EligibilityEvaluator {
    fun assess(
        offer: Offer, policy: Policy, now: Long, destinationEvidence: DestinationEvidence? = null,
    ): Decision {
        val rules = policy.effective(offer.platform).sharedRules()
        val value = ValueEngine.calculate(offer, policy)
        val reasons = buildList {
            if (!policy.valid()) add("接单规则无效，至少启用一项最低收益要求")
            if (offer.platform !in policy.enabledPlatforms) add("该平台未开启自动接单")
            if (offer.currency != "USD") add("当前规则仅适用于 USD")
            if (!offer.reliable && offer.evidence?.amountVerified != true) add("报价金额或页面身份未确认")
            if (offer.evidence?.source == OfferSource.OCR) add("仅由截图识别，保留手动接单")
            if (now < offer.observedAt || now - offer.observedAt > 10_000) add("报价已过期")
            if (offer.amountCents == null || offer.amountCents <= 0) add("缺少有效报价")
            else if (rules.enabled(RuleCondition.MINIMUM_AMOUNT) && offer.amountCents < rules.minimumCents) {
                add("报价低于最低金额")
            }
            if (rules.enabled(RuleCondition.MINIMUM_PER_MILE)) {
                if (value.centsPerMile == null) add("缺少可信总里程")
                else if (value.centsPerMile + 1e-8 < rules.minimumCentsPerMile) add("每英里报价不达标")
            }
            if (rules.enabled(RuleCondition.MINIMUM_HOURLY)) {
                val afterCost = rules.hourlyBasis == HourlyBasis.AFTER_VEHICLE_COST
                val hourly = if (afterCost) value.contributionCentsPerHour else value.grossCentsPerHour
                if (hourly == null) add(if (afterCost) "缺少可信总里程或总耗时，无法计算成本后时薪" else "缺少可信总耗时")
                else if (hourly + 1e-8 < rules.minimumCentsPerHour) {
                    add(if (afterCost) "扣车辆成本后时薪不达标" else "预计毛时薪不达标")
                }
            }
            addAll(RegionalEligibilityEvaluator.reasons(offer, rules.regionalRequirement, destinationEvidence, now))
            addAll(routeLimits(offer, rules))
            if (offer.isStacked && !rules.allowStacked) add("未允许自动接叠单")
            if (offer.isAddon) {
                if (!rules.allowAddon) add("未允许自动接追加单")
                else if (offer.evidence?.incrementalRouteVerified != true) add("追加单增量收入及路线尚未确认")
                else add("追加单的配送上下文复核尚未接入，保留手动接单")
            }
            addAll(excluded(offer.destination, rules.excludedDestinations, "目的地"))
            addAll(excluded(offer.merchant, rules.excludedMerchants, "商家"))
        }.distinct()
        return Decision(reasons.isEmpty(), reasons, value, policy.version)
    }

    private fun routeLimits(offer: Offer, rules: AcceptanceRules) = buildList {
        if (rules.enabled(RuleCondition.MAXIMUM_DISTANCE)) {
            val distance = offer.distanceMeters
            if (!offer.hasVerifiedDistance() || distance == null || distance <= 0) add("缺少可信总里程")
            else if (distance > rules.maximumMeters) add("总里程超过上限")
        }
        if (rules.enabled(RuleCondition.MAXIMUM_DURATION)) {
            val duration = offer.durationSeconds
            if (!offer.hasVerifiedDuration() || duration == null || duration <= 0) add("缺少可信总耗时")
            else if (duration > rules.maximumSeconds) add("订单耗时超过上限")
        }
        val pickupLimit = rules.maximumPickupMeters
        if (rules.enabled(RuleCondition.MAXIMUM_PICKUP) && pickupLimit != null) {
            val pickup = offer.pickupMeters
            if (pickup == null || pickup < 0) add("缺少取餐距离")
            else if (pickup > pickupLimit) add("取餐距离超过上限")
        }
    }

    private fun excluded(value: String?, keywords: Set<String>, label: String): List<String> {
        if (keywords.isEmpty()) return emptyList()
        if (value.isNullOrBlank()) return listOf("无法检查${label}限制")
        return if (keywords.any { value.contains(it, ignoreCase = true) }) listOf("${label}位于排除列表")
            else emptyList()
    }
}
