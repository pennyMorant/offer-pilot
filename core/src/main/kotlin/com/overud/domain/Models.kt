package com.overud.domain

import java.security.MessageDigest

enum class Platform(val label: String, val packageName: String) {
    UBER_EATS("Uber Eats", "com.ubercab.driver"),
    DOORDASH("DoorDash", "com.doordash.driverapp");
    companion object { fun fromPackage(value: String) = entries.find { it.packageName == value } }
}

data class Offer(
    val id: String,
    val platform: Platform,
    val observedAt: Long,
    val amountCents: Long?,
    val distanceMeters: Long?,
    val durationSeconds: Long?,
    val merchant: String? = null,
    val destination: String? = null,
    val pickupMeters: Long? = null,
    val currency: String = "USD",
    val isStacked: Boolean = false,
    val isAddon: Boolean = false,
    val reliable: Boolean = false,
    val rawText: String = "",
)

data class Policy(
    val version: Long = 1,
    val enabledPlatforms: Set<Platform> = Platform.entries.toSet(),
    val minimumCents: Long = 800,
    val minimumCentsPerMile: Long = 200,
    val minimumCentsPerHour: Long = 2500,
    val maximumMeters: Long = 16093,
    val maximumSeconds: Long = 2700,
    val maximumPickupMeters: Long? = null,
    val costCentsPerMile: Long = 35,
    val allowStacked: Boolean = false,
    val allowAddon: Boolean = false,
    val excludedDestinations: Set<String> = emptySet(),
) {
    fun valid() = minimumCents > 0 && minimumCentsPerMile > 0 && minimumCentsPerHour > 0 &&
        maximumMeters > 0 && maximumSeconds > 0 && costCentsPerMile >= 0 &&
        (maximumPickupMeters == null || maximumPickupMeters > 0) && enabledPlatforms.isNotEmpty()
}

data class Valuation(val centsPerMile: Double?, val grossCentsPerHour: Double?, val contributionCents: Double?)
data class Decision(val eligible: Boolean, val reasons: List<String>, val valuation: Valuation, val policyVersion: Long)

object ValueEngine {
    const val METERS_PER_MILE = 1609.344
    fun assess(offer: Offer, policy: Policy, now: Long): Decision {
        val miles = offer.distanceMeters?.takeIf { it > 0 }?.div(METERS_PER_MILE)
        val seconds = offer.durationSeconds?.takeIf { it > 0 }
        val value = Valuation(
            if (miles != null && offer.amountCents != null) offer.amountCents / miles else null,
            if (seconds != null && offer.amountCents != null) offer.amountCents * 3600.0 / seconds else null,
            if (miles != null && offer.amountCents != null) offer.amountCents - miles * policy.costCentsPerMile else null,
        )
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

fun fingerprint(platform: Platform, text: String): String = MessageDigest.getInstance("SHA-256")
    .digest((platform.name + text.lowercase().replace(Regex("\\s+"), " ").trim()).toByteArray())
    .take(12).joinToString("") { "%02x".format(it) }
