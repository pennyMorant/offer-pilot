package com.overud.platform

import com.overud.model.OfferService

/** The Exclusive offer card, not arbitrary prices or route labels on the surrounding map. */
internal data class UberCard(
    val kind: OfferService, val canonicalText: String, val pickup: String?, val destination: String?,
)

internal object UberOfferCard {
    private val header = Regex("^(Package|Delivery)(?:\\s+Exclusive)?$", RegexOption.IGNORE_CASE)
    private val money = Regex("^(?:US\\$|\\$)\\s*\\d{1,4}\\.\\d{2}$")
    private val route = Regex("^(\\d{1,3}\\s*min)\\s*[·•.・]?\\s*(\\d{1,3}(?:\\.\\d{1,2})?\\s*mi)$", RegexOption.IGNORE_CASE)
    private val guarantee = Regex("^Guaranteed\\s*\\(incl\\.?\\s*tip\\)$", RegexOption.IGNORE_CASE)
    private val decoration = Regex("^(?:[★⭐]?\\s*[0-5]\\.\\d{2}(?:\\s+Verified)?|Verified|[×✕x])$", RegexOption.IGNORE_CASE)
    private val postal = Regex("^\\d{5}(?:-\\d{4})?(?:,?\\s*(?:USA|US))?$", RegexOption.IGNORE_CASE)

    fun kind(text: String): OfferService = lines(text).mapNotNull { header.matchEntire(it) }
        .singleOrNull()?.groupValues?.get(1)?.let {
            if (it.equals("Package", true)) OfferService.PACKAGE else OfferService.DELIVERY
        } ?: OfferService.UNKNOWN

    fun hasAnchors(text: String): Boolean {
        val lines = lines(text)
        return lines.count { header.matches(it) } == 1 && lines.count { it.equals("Accept", true) } == 1 &&
            lines.count { money.matches(it) } == 1
    }

    fun read(text: String): UberCard? {
        val lines = lines(text)
        val start = lines.indices.singleOrNull { header.matches(lines[it]) } ?: return null
        val end = lines.indices.singleOrNull { lines[it].equals("Accept", true) } ?: return null
        if (end <= start) return null
        val card = lines.subList(start, end + 1)
        if (card.none { it.contains("Exclusive", true) }) return null
        val price = card.indices.singleOrNull { money.matches(card[it]) } ?: return null
        if (price !in 1..4) return null
        val guaranteeIndex = card.indices.singleOrNull { guarantee.matches(card[it]) } ?: return null
        if (guaranteeIndex !in price + 2..price + 3) return null
        val metrics = route.matchEntire(card.subList(price + 1, guaranteeIndex).joinToString(" ")) ?: return null
        val kind = if (header.matchEntire(card.first())!!.groupValues[1].equals("Package", true))
            OfferService.PACKAGE else OfferService.DELIVERY
        val locations = locations(card.subList(guaranteeIndex + 1, card.lastIndex), kind)
        val canonical = buildList {
            add(card[price])
            add("Total time: ${metrics.groupValues[1]}")
            add("Total distance: ${metrics.groupValues[2]}")
            locations?.first?.let { add("${if (kind == OfferService.PACKAGE) "Pickup" else "Merchant"}: $it") }
            locations?.second?.let { add("Destination: $it") }
            add("Accept")
        }.joinToString("\n")
        return UberCard(kind, canonical, locations?.first?.takeIf { kind == OfferService.PACKAGE }, locations?.second)
    }

    private fun locations(raw: List<String>, kind: OfferService): Pair<String, String>? {
        val rows = raw.filterNot { decoration.matches(it) }
        if (rows.size !in 2..4 || rows.any { it.length > 240 || '$' in it }) return null
        val pickup = mutableListOf(rows.first())
        var index = 1
        if (kind == OfferService.PACKAGE && postal.matches(rows[index])) pickup += rows[index++]
        val destination = rows.drop(index)
        // Only a two-row location layout or a postcode continuation is supported here.
        if (destination.size != 1 || !destination.single().contains(',')) return null
        if (kind == OfferService.PACKAGE && !Regex("^\\d+\\s+\\S").containsMatchIn(pickup.first())) return null
        return pickup.joinToString(" ") to destination.single()
    }

    private fun lines(text: String) = text.lines().map {
        it.trim().replace(Regex("\\s+"), " ")
            .replace(Regex("^(Package|Delivery)(\\s+Exclusive)?\\s+[×✕x]$", RegexOption.IGNORE_CASE), "$1$2")
    }
        .filter(String::isNotBlank)
}
