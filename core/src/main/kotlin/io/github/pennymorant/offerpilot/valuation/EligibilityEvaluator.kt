package io.github.pennymorant.offerpilot.valuation

import io.github.pennymorant.offerpilot.model.*

object EligibilityEvaluator {
    fun assess(
        offer: Offer, policy: Policy, now: Long, destinationEvidence: DestinationEvidence? = null,
        calibration: DurationCalibration? = null,
    ): Decision {
        val rules = policy.effective(offer.platform).sharedRules()
        val value = ValueEngine.calculate(offer, policy, calibration, destinationEvidence)
        val reasons = buildList {
            if (!policy.valid()) add("接单规则无效，至少启用一项最低收益要求")
            if (offer.platform !in policy.enabledPlatforms) add("该平台未开启自动接单")
            if (offer.currency != "USD") add("当前规则仅适用于 USD")
            when (value.confidence) {
                EvaluationConfidence.LOW -> add("报价仅由截图或不完整页面识别，保留手动接单")
                EvaluationConfidence.UNKNOWN -> add("报价页面或核心经济字段未确认")
                EvaluationConfidence.HIGH, EvaluationConfidence.MEDIUM -> Unit
            }
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
            if (rules.enabled(RuleCondition.MINIMUM_CONTRIBUTION)) {
                val contribution = value.contributionCents
                if (contribution == null) add("缺少可信报价或总里程，无法计算成本后贡献")
                else if (contribution + 1e-8 < rules.minimumContributionCents) add("单笔成本后贡献低于最低要求")
            }
            val score = value.score
            if (score == null) add("缺少可信金额、总里程或总耗时，无法完成价值评分")
            else if (score < rules.minimumValueScore) add("订单经济价值评分低于 ${rules.minimumValueScore} 分")
            if (offer.channel == OfferChannel.TRIP_RADAR) {
                if (!rules.allowTripRadar) add("未允许自动抢雷达单")
                else if (rules.radarExtraScore > 0 && score != null) {
                    val radarMin = (rules.minimumValueScore + rules.radarExtraScore).coerceAtMost(100)
                    if (score < radarMin) add("雷达单价值分低于 ${radarMin} 分（不扣AR只抢高分单）")
                }
            }
            if (offer.serviceKind == OfferService.PACKAGE && !rules.allowPackage) add("未允许自动接包裹单")
            if (offer.serviceKind == OfferService.SHOP_AND_PAY) {
                if (!rules.allowShopAndPay) add("未允许自动接商超采购单")
                else rules.maximumShopItems?.let { maxItems ->
                    val count = offer.itemCount
                    if (count == null) add("缺少商超商品件数")
                    else if (count > maxItems) add("商超件数超过上限（最多 ${maxItems} 件）")
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
