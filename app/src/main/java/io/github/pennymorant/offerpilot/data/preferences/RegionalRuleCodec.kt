package io.github.pennymorant.offerpilot.data.preferences

import io.github.pennymorant.offerpilot.model.RegionalRequirement
import org.json.JSONObject

object RegionalRuleCodec {
    fun encode(rule: RegionalRequirement) = JSONObject().apply {
        put("enabled", rule.enabled)
        put("goodPercent", rule.minimumGoodPercent)
        put("samples", rule.minimumClassified)
    }

    fun decode(json: JSONObject) = RegionalRequirement(
        json.getBoolean("enabled"), json.getInt("goodPercent"), json.getLong("samples"),
    ).also { require(it.valid()) }

    fun read(value: String?): RegionalRequirement? = if (value == null) RegionalRequirement()
        else runCatching { decode(JSONObject(value)) }.getOrNull()
}
