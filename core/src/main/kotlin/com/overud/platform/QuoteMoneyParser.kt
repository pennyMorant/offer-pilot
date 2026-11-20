package com.overud.platform

import java.math.BigDecimal

internal data class QuoteMoney(val cents: Long?, val currency: String, val incremental: Boolean)

/** Never choose one amount from multiple monetary values or silently convert another currency. */
internal object QuoteMoneyParser {
    private val money = Regex("(?<![\\p{L}\\d−-])(?:US\\$|\\$)\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)(?![\\d.,])")
    private val headline = Regex("^(?:US\\$|\\$)\\s*(\\d{1,4}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*\\+?(?:\\s*(?:total|guaranteed|incl\\.?.*))?$",
        RegexOption.IGNORE_CASE)
    private val currency = Regex("\\b(USD|CAD|AUD|NZD|SGD|HKD|EUR|GBP)\\b", RegexOption.IGNORE_CASE)
    private val extra = Regex("^(?:additional|extra|incremental|added|\\+)(?: pay| earnings| amount| payout)?\\s*[:：]?\\s*(?:US\\$|\\$)",
        RegexOption.IGNORE_CASE)

    fun parse(text: String): QuoteMoney {
        val matches = money.findAll(text).toList()
        val unit = detectCurrency(text)
        val negative = Regex("[−-]\\s*(?:US\\$|\\$)|(?:US\\$|\\$)\\s*[−-]").containsMatchIn(text)
        val symbolCount = Regex("(?:US\\$|\\$)").findAll(text).count()
        val cents = matches.takeIf { it.size == symbolCount && !negative }?.singleOrNull()?.groupValues?.get(1)
            ?.let(::toCents)
        val incremental = matches.size == 1 && text.lines().any { extra.containsMatchIn(it.trim()) }
        return QuoteMoney(cents, unit, incremental)
    }

    /** Card-scoped extraction: tolerates secondary labels like peak pay or tip notes outside the headline price. */
    fun parseCard(cardText: String, primaryHint: String? = null): QuoteMoney {
        val unit = detectCurrency(cardText)
        val incremental = cardText.lines().any { extra.containsMatchIn(it.trim()) }
        val hinted = primaryHint?.let { money.find(it)?.groupValues?.get(1) }?.let(::toCents)
        if (hinted != null) return QuoteMoney(hinted, unit, incremental)

        val strict = parse(cardText)
        if (strict.cents != null) return strict

        val headlines = cardText.lines().map(String::trim)
            .mapNotNull { headline.matchEntire(it)?.groupValues?.get(1) }
            .mapNotNull(::toCents)
            .distinct()
        if (headlines.size == 1) return QuoteMoney(headlines.single(), unit, incremental)
        return QuoteMoney(null, unit, incremental)
    }

    private fun detectCurrency(text: String): String {
        val currencies = currency.findAll(text).map { it.value.uppercase(java.util.Locale.ROOT) }.toSet()
        return when {
            currencies.isEmpty() -> "USD"
            currencies.size == 1 -> currencies.single()
            else -> "UNKNOWN"
        }
    }

    private fun toCents(raw: String): Long? = runCatching {
        raw.replace(",", "").toBigDecimal().multiply(BigDecimal(100)).longValueExact()
    }.getOrNull()?.takeIf { it in 1..10_000_000 }
}
