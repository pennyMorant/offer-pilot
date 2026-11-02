package com.overud.platform

data class DeliveryLocations(val merchant: String?, val pickup: String?, val destination: String?)

/** Extract only explicitly labelled locations. Unlabelled map text is not an address. */
object DeliveryLocationParser {
    private val merchant = listOf("Merchant", "Restaurant", "Store", "取餐商家", "商家", "餐厅")
    private val pickup = listOf("Pickup", "Pick-up", "Pick up at", "取餐地址")
    private val destination = listOf("Destination", "Drop-off", "Dropoff", "Deliver to", "Delivery address",
        "送达地址", "目的地", "送餐地址")
    private val labels = merchant + pickup + destination
    private val labelPattern = pattern(labels)
    private val action = Regex("^(accept|match|decline|total|接受|接单|拒绝)(\\b|$)", RegexOption.IGNORE_CASE)

    fun parse(text: String): DeliveryLocations {
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        return DeliveryLocations(read(lines, merchant), read(lines, pickup), read(lines, destination))
    }

    private fun read(lines: List<String>, names: List<String>): String? {
        val pattern = pattern(names)
        val matches = lines.mapIndexedNotNull { index, line ->
            val match = pattern.matchEntire(line) ?: return@mapIndexedNotNull null
            val value = match.groupValues[1].ifBlank { lines.getOrNull(index + 1).orEmpty() }
            value.trim().takeIf { it.isNotBlank() && it.length <= 240 &&
                !labelPattern.matches(it) && !action.containsMatchIn(it) &&
                !it.startsWith('$') && !Regex("^\\d+(?:\\.\\d+)?\\s*(mi|km|min)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        }
        return matches.distinctBy { it.lowercase() }.singleOrNull()
    }

    private fun pattern(names: List<String>) = Regex(
        "^(?:${names.joinToString("|") { Regex.escape(it) }})(?:\\s*[:：]\\s*|\\s+|$)(.*)$",
        RegexOption.IGNORE_CASE,
    )
}
