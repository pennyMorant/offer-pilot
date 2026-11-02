package com.overud.platform

import com.overud.model.*
import java.util.Locale

/** Conflicting evidence is explicit; prices and action words alone never override a history page. */
object PageClassifier {
    private val money = Regex("(?:US\\$|\\$)\\s*\\d")

    fun classify(platform: Platform, text: String, rules: PlatformPageRules, source: OfferSource): PlatformPage {
        val lines = text.lines().map(::normalize).filter(String::isNotBlank).toSet()
        val signals = buildSet {
            if (lines.any { it in rules.quoteActions }) add(PageSignal.QUOTE_ACTION)
            if (money.containsMatchIn(text)) add(PageSignal.PRICE)
            if (lines.any { it in rules.confirmation }) add(PageSignal.ACCEPT_CONFIRMATION)
            if (lines.any { it in rules.accepted }) add(PageSignal.ACCEPTED)
            if (lines.any { it in rules.pickup }) add(PageSignal.PICKUP)
            if (lines.any { it in rules.delivery }) add(PageSignal.DELIVERY)
            if (lines.any { it in rules.completed }) add(PageSignal.COMPLETED)
            if (lines.any { it in rules.cancelled }) add(PageSignal.CANCELLED)
            if (lines.any { it in rules.offline }) add(PageSignal.OFFLINE)
            if (lines.any { it in rules.history }) add(PageSignal.HISTORY)
            if (lines.any { line -> rules.addon.any { line == it || line.startsWith("$it:") } }) add(PageSignal.ADDON)
        }
        return PlatformPage(platform, decide(signals), source, signals)
    }

    private fun decide(signals: Set<PageSignal>): PlatformPageType {
        if (PageSignal.HISTORY in signals) return PlatformPageType.HISTORY
        val quote = PageSignal.QUOTE_ACTION in signals && PageSignal.PRICE in signals
        val phases = buildSet {
            if (PageSignal.ACCEPT_CONFIRMATION in signals) add(PlatformPageType.ACCEPT_CONFIRMATION)
            if (PageSignal.PICKUP in signals) add(PlatformPageType.PICKUP)
            if (PageSignal.DELIVERY in signals) add(PlatformPageType.DELIVERY)
            if (PageSignal.COMPLETED in signals) add(PlatformPageType.COMPLETED)
            if (PageSignal.CANCELLED in signals) add(PlatformPageType.CANCELLED)
            if (PageSignal.OFFLINE in signals) add(PlatformPageType.OFFLINE)
        }
        if (phases.size > 1 || (quote && phases.isNotEmpty())) return PlatformPageType.AMBIGUOUS
        if (quote && PageSignal.ACCEPTED in signals) return PlatformPageType.AMBIGUOUS
        if (quote) return if (PageSignal.ADDON in signals) PlatformPageType.ADDON_OFFER else PlatformPageType.OFFER
        if (phases.isNotEmpty()) return phases.single()
        if (PageSignal.ACCEPTED in signals) return PlatformPageType.ACCEPTED
        return PlatformPageType.UNKNOWN
    }

    private fun normalize(value: String) = value.trim().trimEnd('!', '.', '。', '！', ':', '：')
        .replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
