package com.overud.platform

internal data class RouteMetrics(
    val meters: Long?, val seconds: Long?, val pickupMeters: Long?,
    val distanceVerified: Boolean, val durationVerified: Boolean,
)

/** Labels bind to their own metric, never to an unrelated Total payout elsewhere on the page. */
internal object RouteMetricsParser {
    private enum class Scope { UNKNOWN, TOTAL, INCREMENTAL, PICKUP }
    private data class Metric(val value: Long?, val scope: Scope)
    private data class Selection(val value: Long?, val verified: Boolean)
    private val distance = Regex("(?<![\\d.])(\\d+(?:\\.\\d+)?)\\s*(mi(?:les?)?|km)\\b", RegexOption.IGNORE_CASE)
    private val duration = Regex("(?<![\\d.])(?:(\\d+)\\s*(?:hours?|hrs?|h)\\s*)?(\\d+)\\s*min(?:ute)?s?\\b|" +
        "(?<![\\d.])(\\d+)\\s*(?:hours?|hrs?|h)\\b", RegexOption.IGNORE_CASE)
    private val total = "(?:total(?: route| trip)?|全程|总计)"
    private val incremental = "(?:additional|extra|incremental|added|新增|额外)"

    fun parse(text: String, addon: Boolean): RouteMetrics {
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        val distances = mutableListOf<Metric>()
        val durations = mutableListOf<Metric>()
        lines.forEachIndexed { index, line ->
            val previous = lines.getOrNull(index - 1).orEmpty()
            distance.findAll(line).forEach { match ->
                val factor = if (match.groupValues[2].startsWith("mi", true)) 1609.344 else 1000.0
                val meters = match.groupValues[1].toDoubleOrNull()?.times(factor)
                    ?.takeIf { it.isFinite() && it in 0.0..1_000_000.0 }?.toLong()
                distances += Metric(meters, scope(line, previous, true))
            }
            duration.findAll(line).forEach { match ->
                val hours = (match.groupValues[1].ifBlank { match.groupValues[3] }).ifBlank { "0" }.toLongOrNull()
                val minutes = match.groupValues[2].ifBlank { "0" }.toLongOrNull()
                val seconds = if (hours != null && hours in 0..24 && minutes != null && minutes in 0..1440)
                    (hours * 3600 + minutes * 60).takeIf { it in 1..86400 } else null
                durations += Metric(seconds, scope(line, previous, false))
            }
        }
        val expected = if (addon) Scope.INCREMENTAL else Scope.TOTAL
        val miles = select(distances.filter { it.scope != Scope.PICKUP }, expected)
        val time = select(durations, expected)
        val pickup = distances.filter { it.scope == Scope.PICKUP }.map { it.value }.distinct().singleOrNull()
        return RouteMetrics(miles.value?.takeIf { it > 0 }, time.value, pickup,
            miles.verified && miles.value != null && miles.value > 0, time.verified)
    }

    private fun select(metrics: List<Metric>, expected: Scope): Selection {
        val labelled = metrics.filter { it.scope == expected }
        val choices = (if (labelled.isNotEmpty()) labelled else metrics.filter { it.scope == Scope.UNKNOWN })
            .map { it.value }.distinct()
        val value = choices.singleOrNull()
        return Selection(value, labelled.isNotEmpty() && choices.size == 1 && value != null)
    }

    private fun scope(line: String, previous: String, isDistance: Boolean): Scope {
        val metric = if (isDistance) "(?:distance|里程|距离)" else "(?:time|duration|耗时|时间)"
        val other = if (isDistance) "(?:time|duration|耗时|时间)" else "(?:distance|里程|距离)"
        if (Regex("^$total\\s*$other\\b", RegexOption.IGNORE_CASE).containsMatchIn(line)) return Scope.UNKNOWN
        if (Regex("^$incremental\\s*$other\\b", RegexOption.IGNORE_CASE).containsMatchIn(line)) return Scope.UNKNOWN
        if (isDistance && labelled(line, previous, "(?:pickup(?: distance)?|取餐距离)")) return Scope.PICKUP
        if (labelled(line, previous, "$total\\s*$metric|总$metric")) return Scope.TOTAL
        if (labelled(line, previous, "$incremental(?:\\s+$metric)?")) return Scope.INCREMENTAL
        if (Regex("^$total\\s*[:：]?\\s*\\d", RegexOption.IGNORE_CASE).containsMatchIn(line)) return Scope.TOTAL
        if (labelled(line, previous, "(?:total route|total trip|全程路线)")) return Scope.TOTAL
        return Scope.UNKNOWN
    }

    private fun labelled(line: String, previous: String, label: String): Boolean {
        val prefix = Regex("^(?:$label)\\s*[:：]?\\s*\\d", RegexOption.IGNORE_CASE)
        val standalone = Regex("^(?:$label)\\s*[:：]?\\s*$", RegexOption.IGNORE_CASE)
        return prefix.containsMatchIn(line) || (standalone.matches(previous) &&
            Regex("^\\d").containsMatchIn(line) && !line.contains('$'))
    }
}
