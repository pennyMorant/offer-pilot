package com.overud.valuation

import com.overud.model.*

object EligibilityEvaluator {
    fun assess(offer: Offer, policy: Policy, now: Long): Decision {
        val value = ValueEngine.calculate(offer, policy)
        val reasons = buildList {
            if (!policy.valid()) add("接单规则无效")
            if (offer.platform !in policy.enabledPlatforms) add("该平台未开启自动接单")
            if (offer.currency != "USD") add("当前规则仅适用于 USD")
            if (!offer.reliable) add("报价字段或里程、时间口径尚未确认")
            if (now < offer.observedAt || now - offer.observedAt > 10_000) add("报价已过期")
            if (offer.amountCents == null || offer.amountCents <= 0) add("缺少有效报价")
            else if (offer.amountCents < policy.minimumCents) add("报价低于最低金额")
            if (value.centsPerMile == null) add("缺少总里程")
            else if (value.centsPerMile + 1e-8 < policy.minimumCentsPerMile) add("每英里报价不达标")
            if (value.grossCentsPerHour == null) add("缺少总耗时")
            else if (value.grossCentsPerHour + 1e-8 < policy.minimumCentsPerHour) add("预计时薪不达标")
            if (offer.distanceMeters != null && offer.distanceMeters > policy.maximumMeters) add("总里程超过上限")
            if (offer.durationSeconds != null && offer.durationSeconds > policy.maximumSeconds) add("订单耗时超过上限")
            policy.maximumPickupMeters?.let { maximum ->
                if (offer.pickupMeters == null) add("缺少取餐距离")
                else if (offer.pickupMeters > maximum) add("取餐距离超过上限")
            }
            if (offer.isStacked && !policy.allowStacked) add("未允许自动接叠单")
            if (offer.isAddon && !policy.allowAddon) add("未允许自动接追加单")
            if (policy.excludedDestinations.isNotEmpty()) {
                if (offer.destination == null) add("无法检查目的地区域限制")
                else if (policy.excludedDestinations.any { offer.destination.contains(it, ignoreCase = true) }) add("目的地位于排除区域")
            }
        }
        return Decision(reasons.isEmpty(), reasons, value, policy.version)
    }
}
