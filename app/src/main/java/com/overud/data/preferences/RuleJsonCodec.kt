package com.overud.data.preferences

import com.overud.model.*
import org.json.JSONArray
import org.json.JSONObject

object RuleJsonCodec {
    fun encode(overrides: Map<Platform, AcceptanceRules>): String = JSONObject().apply {
        overrides.forEach { (platform, rules) -> put(platform.name, encodeRules(rules)) }
    }.toString()

    fun decode(value: String): Map<Platform, AcceptanceRules>? = runCatching {
        val root = JSONObject(value)
        buildMap {
            for (key in root.keys()) {
                val platform = Platform.entries.find { it.name == key } ?: error("Unknown platform")
                put(platform, decodeRules(root.getJSONObject(key)))
            }
        }
    }.getOrNull()

    private fun encodeRules(rules: AcceptanceRules) = JSONObject().apply {
        put("min", rules.minimumCents)
        put("mile", rules.minimumCentsPerMile)
        put("hour", rules.minimumCentsPerHour)
        put("minContribution", rules.minimumContributionCents)
        put("targetContribution", rules.targetContributionCents)
        put("targetContributionMile", rules.targetContributionCentsPerMile)
        put("targetContributionHour", rules.targetContributionCentsPerHour)
        put("minimumValueScore", rules.minimumValueScore)
        put("distance", rules.maximumMeters)
        put("time", rules.maximumSeconds)
        put("pickup", rules.maximumPickupMeters ?: JSONObject.NULL)
        put("cost", rules.costCentsPerMile)
        put("hourlyBasis", rules.hourlyBasis.name)
        put("region", RegionalRuleCodec.encode(rules.regionalRequirement))
        put("stack", rules.allowStacked)
        put("addon", rules.allowAddon)
        put("destinations", JSONArray(rules.excludedDestinations.toList()))
        put("merchants", JSONArray(rules.excludedMerchants.toList()))
        put("disabled", JSONArray(rules.disabledConditions.map { it.name }))
    }

    private fun decodeRules(json: JSONObject): AcceptanceRules {
        val disabled = strings(json.getJSONArray("disabled")).map { name ->
            RuleCondition.entries.find { it.name == name } ?: error("Unknown condition")
        }.toSet()
        return AcceptanceRules(
            minimumCents = json.getLong("min"),
            minimumCentsPerMile = json.getLong("mile"),
            minimumCentsPerHour = json.getLong("hour"),
            minimumContributionCents = json.optLong("minContribution", 500),
            targetContributionCents = json.optLong("targetContribution", 800),
            targetContributionCentsPerMile = json.optLong("targetContributionMile", 165),
            targetContributionCentsPerHour = json.optLong("targetContributionHour", 2000),
            minimumValueScore = json.optInt("minimumValueScore", 70),
            maximumMeters = json.getLong("distance"),
            maximumSeconds = json.getLong("time"),
            maximumPickupMeters = if (json.isNull("pickup")) null else json.getLong("pickup"),
            costCentsPerMile = json.getLong("cost"),
            hourlyBasis = if (json.has("hourlyBasis")) HourlyBasis.valueOf(json.getString("hourlyBasis")) else HourlyBasis.GROSS,
            regionalRequirement = if (json.has("region")) RegionalRuleCodec.decode(json.getJSONObject("region")) else RegionalRequirement(),
            allowStacked = json.getBoolean("stack"),
            allowAddon = json.getBoolean("addon"),
            excludedDestinations = strings(json.getJSONArray("destinations")),
            excludedMerchants = strings(json.getJSONArray("merchants")),
            disabledConditions = disabled,
        ).also { require(it.valid()) }
    }

    private fun strings(array: JSONArray): Set<String> =
        (0 until array.length()).map { array.getString(it) }.toSet()
}
