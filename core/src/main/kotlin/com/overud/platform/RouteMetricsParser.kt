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
    private val label = "(?:total(?:\\s+(?:route|trip|distance|time|duration))?|" +
        "(?:additional|extra|incremental|added)(?:\\s+(?:route|distance|time|duration))?|pickup(?:\\s+distance)?|" +
        "总里程|总距离|总耗时|总时间|全程路线|全程|总计|取餐距离|(?:新增|额外)(?:里程|距离|耗时|时间)?)"
    private val marker = Regex("(?:^|\\s)($label)\\s*[:：]?\\s*(?=\\d)", RegexOption.IGNORE_CASE)
    private val trailingTotal = Regex("^\\s*(?:total|总计|全程)\\b", RegexOption.IGNORE_CASE)
    private val standalone = Regex("^($label)\\s*[:：]?\\s*$", RegexOption.IGNORE_CASE)
    private val compactTimeDist = Regex(
        "(?:(\\d+)\\s*(?:hours?|hrs?|h)\\s*)?(\\d+)\\s*min(?:ute)?s?\\s*(?:\\(|[·•.\\-|,*]|\\s+)\\s*(\\d+(?:\\.\\d+)?)\\s*(mi(?:les?)?|km)\\)?",
        RegexOption.IGNORE_CASE,
    )
    private val compactDistTime = Regex(
        "(\\d+(?:\\.\\d+)?)\\s*(mi(?:les?)?|km)\\s*(?:\\(|[·•.\\-|,*]|\\s+)\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h)\\s*)?(\\d+)\\s*min(?:ute)?s?\\)?",
        RegexOption.IGNORE_CASE,
    )
    private val deliverBy = Regex(
        "(?:deliver\\s+by|送达时间)\\s*[:：]?\\s*(\\d{1,2}):(\\d{2})(?:\\s*(am|pm))?",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String, addon: Boolean, cardScoped: Boolean = false, nowMillis: Long = 0L): RouteMetrics {
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        val distances = mutableListOf<Metric>()
        val durations = mutableListOf<Metric>()
        var compactMeters: Long? = null
        var compactSeconds: Long? = null

        lines.forEachIndexed { index, line ->
            val previous = lines.getOrNull(index - 1).orEmpty()
            if (compactMeters == null && compactSeconds == null) {
                parseCompact(line)?.let { (m, s) -> compactMeters = m; compactSeconds = s }
            }
            distance.findAll(line).forEach { match ->
                val factor = if (match.groupValues[2].startsWith("mi", true)) 1609.344 else 1000.0
                val negative = Regex("[−-]\\s*$").containsMatchIn(line.take(match.range.first))
                val meters = match.groupValues[1].toDoubleOrNull()?.times(factor)
                    ?.takeIf { !negative && it.isFinite() && it in 0.0..1_000_000.0 }?.toLong()
                val suffix = line.substring(match.range.last + 1)
                val sc = if (trailingTotal.containsMatchIn(suffix)) Scope.TOTAL
                    else scope(line.take(match.range.last + 1), previous, true)
                distances += Metric(meters, sc)
            }
            duration.findAll(line).forEach { match ->
                val hours = (match.groupValues[1].ifBlank { match.groupValues[3] }).ifBlank { "0" }.toLongOrNull()
                val minutes = match.groupValues[2].ifBlank { "0" }.toLongOrNull()
                val negative = Regex("[−-]\\s*$").containsMatchIn(line.take(match.range.first))
                val seconds = if (!negative && hours != null && hours in 0..24 && minutes != null && minutes in 0..1440)
                    (hours * 3600 + minutes * 60).takeIf { it in 1..86400 } else null
                val suffix = line.substring(match.range.last + 1)
                val sc = if (trailingTotal.containsMatchIn(suffix)) Scope.TOTAL
                    else scope(line.take(match.range.last + 1), previous, false)
                durations += Metric(seconds, sc)
            }
        }
        val expected = if (addon) Scope.INCREMENTAL else Scope.TOTAL
        val miles = select(distances.filter { it.scope != Scope.PICKUP }, expected, cardScoped)
        val time = select(durations, expected, cardScoped)
        val pickup = distances.filter { it.scope == Scope.PICKUP }.map { it.value }.distinct().singleOrNull()
        val finalMeters = (miles.value ?: compactMeters)?.takeIf { it > 0 }
        val deliverBySeconds = if (time.value == null && compactSeconds == null && cardScoped && nowMillis > 0L) {
            parseDeliverBySeconds(text, nowMillis)
        } else null
        val finalSeconds = time.value ?: compactSeconds ?: deliverBySeconds
        val distOk = (miles.verified || (cardScoped && compactMeters != null) || (cardScoped && miles.value != null)) && finalMeters != null
        val timeOk = (time.verified || (cardScoped && (compactSeconds != null || deliverBySeconds != null)) ||
            (cardScoped && time.value != null)) && finalSeconds != null
        return RouteMetrics(finalMeters, finalSeconds, pickup, distOk, timeOk)
    }

    private fun parseDeliverBySeconds(text: String, nowMillis: Long): Long? {
        val m = deliverBy.find(text) ?: return null
        val rawHour = m.groupValues[1].toIntOrNull() ?: return null
        val minute = m.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val meridiem = m.groupValues[3].lowercase(java.util.Locale.ROOT)
        val hour24 = when (meridiem) {
            "am" -> if (rawHour in 1..12) rawHour % 12 else return null
            "pm" -> if (rawHour in 1..12) (rawHour % 12) + 12 else return null
            else -> if (rawHour in 0..23) rawHour else return null
        }
        val zone = java.time.ZoneId.systemDefault()
        val nowLocal = java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone)
        var target = nowLocal.withHour(hour24).withMinute(minute).withSecond(0).withNano(0)
        if (target.isBefore(nowLocal.minusMinutes(5))) target = target.plusDays(1)
        val diff = java.time.Duration.between(nowLocal, target).seconds
        return diff.takeIf { it in 300..10_800 }
    }

    private fun parseCompact(line: String): Pair<Long, Long>? {
        compactTimeDist.find(line)?.let { m ->
            val h = m.groupValues[1].ifBlank { "0" }.toLongOrNull() ?: return null
            val min = m.groupValues[2].toLongOrNull() ?: return null
            val factor = if (m.groupValues[4].startsWith("mi", true)) 1609.344 else 1000.0
            val meters = m.groupValues[3].toDoubleOrNull()?.times(factor)?.toLong()?.takeIf { it in 1..1_000_000 } ?: return null
            val secs = (h * 3600 + min * 60).takeIf { it in 1..86400 } ?: return null
            return meters to secs
        }
        compactDistTime.find(line)?.let { m ->
            val factor = if (m.groupValues[2].startsWith("mi", true)) 1609.344 else 1000.0
            val meters = m.groupValues[1].toDoubleOrNull()?.times(factor)?.toLong()?.takeIf { it in 1..1_000_000 } ?: return null
            val h = m.groupValues[3].ifBlank { "0" }.toLongOrNull() ?: return null
            val min = m.groupValues[4].toLongOrNull() ?: return null
            val secs = (h * 3600 + min * 60).takeIf { it in 1..86400 } ?: return null
            return meters to secs
        }
        return null
    }

    private fun select(metrics: List<Metric>, expected: Scope, cardScoped: Boolean): Selection {
        val labelled = metrics.filter { it.scope == expected }
        val choices = (if (labelled.isNotEmpty()) labelled else metrics.filter { it.scope == Scope.UNKNOWN })
            .map { it.value }.distinct()
        val value = choices.singleOrNull()
        val verified = (labelled.isNotEmpty() || cardScoped) && choices.size == 1 && value != null
        return Selection(value, verified)
    }

    private fun scope(prefix: String, previous: String, isDistance: Boolean): Scope {
        val inline = marker.findAll(prefix).lastOrNull()?.groupValues?.get(1)
        val preceding = standalone.matchEntire(previous)?.groupValues?.get(1)
            ?.takeUnless { it.lowercase(java.util.Locale.ROOT) in setOf("total", "总计", "全程") }
        val name = (inline ?: preceding)?.lowercase(java.util.Locale.ROOT) ?: return Scope.UNKNOWN
        val distanceLabel = Regex("distance|里程|距离").containsMatchIn(name)
        val timeLabel = Regex("time|duration|耗时|时间").containsMatchIn(name)
        if ((distanceLabel && !isDistance) || (timeLabel && isDistance)) return Scope.UNKNOWN
        return when {
            name.startsWith("pickup") || name == "取餐距离" -> if (isDistance) Scope.PICKUP else Scope.UNKNOWN
            name.startsWith("total") || name.startsWith("总") || name.startsWith("全程") -> Scope.TOTAL
            else -> Scope.INCREMENTAL
        }
    }
}
