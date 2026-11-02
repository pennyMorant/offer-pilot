package com.overud.platform

import com.overud.model.*
import java.math.BigDecimal
import java.math.RoundingMode

/** Conservative parser: ambiguous screens are display-only, never eligible for automation. */
abstract class TextOfferParser(
    private val platform: Platform,
    private val accept: Regex,
) : OfferParser {
    private val money = Regex("(?:US\\$|\\$)\\s*(\\d{1,4}(?:,\\d{3})*(?:\\.\\d{2})?)(?![\\d.])")
    private val distance = Regex("(\\d+(?:\\.\\d+)?)\\s*(mi(?:les?)?|km)\\b", RegexOption.IGNORE_CASE)
    private val minutes = Regex("(\\d+)\\s*(?:min(?:ute)?s?)\\b", RegexOption.IGNORE_CASE)

    override fun parse(text: String, now: Long): Offer? {
        val amounts = money.findAll(text).toList()
        if (amounts.isEmpty() || !accept.containsMatchIn(text)) return null
        val distances = distance.findAll(text).toList()
        val times = minutes.findAll(text).toList()
        val cents = amounts.singleOrNull()?.groupValues?.get(1)?.replace(",", "")
            ?.toBigDecimalOrNull()?.multiply(BigDecimal(100))?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
        val meters = distances.singleOrNull()?.let {
            val factor = if (it.groupValues[2].startsWith("mi", true)) 1609.344 else 1000.0
            (it.groupValues[1].toDouble() * factor).toLong()
        }
        val seconds = times.singleOrNull()?.groupValues?.get(1)?.toLongOrNull()?.times(60)
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val totalExplicit = Regex("total|总计|全程", RegexOption.IGNORE_CASE).containsMatchIn(text)
        return Offer(
            id = fingerprint(platform, listOf(cents, meters, seconds,
                lines.find { it.startsWith("Merchant:", true) }, lines.find { it.startsWith("Destination:", true) }).joinToString("|")),
            platform = platform, observedAt = now,
            amountCents = cents, distanceMeters = meters, durationSeconds = seconds,
            merchant = lines.find { it.startsWith("Merchant:", true) }?.substringAfter(':')?.trim(),
            destination = lines.find { it.startsWith("Destination:", true) }?.substringAfter(':')?.trim(),
            isStacked = Regex("[2-9] (orders|deliveries)|叠单", RegexOption.IGNORE_CASE).containsMatchIn(text),
            isAddon = Regex("add.?on|add a delivery|追加", RegexOption.IGNORE_CASE).containsMatchIn(text),
            reliable = cents != null && meters != null && seconds != null && totalExplicit,
            rawText = text,
            evidence = QuoteEvidence(amountVerified = cents != null, totalsVerified = totalExplicit),
        )
    }
}
