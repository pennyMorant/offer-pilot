package com.overud.platform

import java.math.BigDecimal

internal data class QuoteMoney(val cents: Long?, val currency: String, val incremental: Boolean)

/** Never choose one amount from multiple monetary values or silently convert another currency. */
internal object QuoteMoneyParser {
    private val money = Regex("(?<![\\p{L}\\d−-])(?:US\\$|\\$)\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)(?![\\d.,])")
    private val currency = Regex("\\b(USD|CAD|AUD|NZD|SGD|HKD|EUR|GBP)\\b", RegexOption.IGNORE_CASE)
    private val extra = Regex("^(?:additional|extra|incremental|added)(?: pay| earnings| amount| payout)?\\s*[:：]?\\s*(?:US\\$|\\$)",
        RegexOption.IGNORE_CASE)

    fun parse(text: String): QuoteMoney {
        val matches = money.findAll(text).toList()
        val currencies = currency.findAll(text).map { it.value.uppercase(java.util.Locale.ROOT) }.toSet()
        val unit = when {
            currencies.isEmpty() -> "USD"
            currencies.size == 1 -> currencies.single()
            else -> "UNKNOWN"
        }
        val negative = Regex("[−-]\\s*(?:US\\$|\\$)|(?:US\\$|\\$)\\s*[−-]").containsMatchIn(text)
        val symbolCount = Regex("(?:US\\$|\\$)").findAll(text).count()
        val cents = matches.takeIf { it.size == symbolCount && !negative }?.singleOrNull()?.groupValues?.get(1)?.replace(",", "")?.let { raw ->
            runCatching { raw.toBigDecimal().multiply(BigDecimal(100)).longValueExact() }.getOrNull()
        }?.takeIf { it in 1..10_000_000 }
        val incremental = matches.size == 1 && text.lines().any { extra.containsMatchIn(it.trim()) }
        return QuoteMoney(cents, unit, incremental)
    }
}
