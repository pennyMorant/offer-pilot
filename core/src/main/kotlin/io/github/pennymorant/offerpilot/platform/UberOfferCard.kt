package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.OfferChannel
import io.github.pennymorant.offerpilot.model.OfferService

/** Flexible Uber Eats card metadata & location extractor supporting Exclusive, Trip Radar, Package & Shop & Pay. */
internal data class UberCard(
    val kind: OfferService,
    val channel: OfferChannel,
    val preferredTag: Boolean,
    val includesTip: Boolean?,
    val itemCount: Int?,
    val merchant: String?,
    val pickup: String?,
    val destination: String?,
    val cardText: String,
)

internal object UberOfferCard {
    private val header = Regex("(?:^|\\s)(Package|Delivery|Shop\\s*&\\s*Pay|Shop\\s+and\\s+Pay|Connect)(?:\\s*\\(\\d+\\))?(?:\\s+Exclusive)?$",
        RegexOption.IGNORE_CASE)
    private val money = Regex("(?:US\\$|\\$)\\s*\\d{1,4}(?:,\\d{3})*(?:\\.\\d{2})?")
    private val route = Regex("\\d+(?:\\.\\d+)?\\s*(?:mi(?:les?)?|km)\\b|\\d+\\s*min(?:ute)?s?\\b", RegexOption.IGNORE_CASE)
    private val action = Regex("^(?:accept|match|decline|接受|接单|拒绝)(?:\\s|$)", RegexOption.IGNORE_CASE)
    private val tipHint = Regex("incl\\.?\\s*(?:expected\\s*)?tip|includes\\s+(?:expected\\s+)?tip|含小费|guaranteed", RegexOption.IGNORE_CASE)
    private val preferred = Regex("preferred\\s+deliver|优选", RegexOption.IGNORE_CASE)
    private val shopHint = Regex("shop\\s*(?:&|and)\\s*(?:pay|deliver)|商超|采购", RegexOption.IGNORE_CASE)
    private val items = Regex("\\b(\\d{1,3})\\s*(?:items?|units?|件)\\b", RegexOption.IGNORE_CASE)
    private val postal = Regex("^\\d{5}(?:-\\d{4})?(?:,?\\s*(?:USA|US))?$", RegexOption.IGNORE_CASE)
    private val decoration = Regex(
        "^(?:[★⭐*☆•·✓✔]?\\s*[0-5]\\.\\d{1,2}(?:\\s*[✓✔v]?\\s*Verified)?|[✓✔v]?\\s*Verified|[×✕x]|Exclusive|Trip\\s+Radar|" +
            "Preferred\\s+Deliver(?:y|ies)|Guaranteed.*|Includes.*tip.*|Package.*|Delivery.*|Shop\\s*&\\s*Pay.*|" +
            "\\d+\\s*(?:items?|units?)|\\d+\\s*s)$",
        RegexOption.IGNORE_CASE,
    )

    fun kind(text: String): OfferService {
        val lines = lines(text)
        if (lines.any { shopHint.containsMatchIn(it) }) return OfferService.SHOP_AND_PAY
        if (lines.any { Regex("\\b(?:Package|Connect)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) && it.length <= 32 }) {
            return OfferService.PACKAGE
        }
        if (lines.any { Regex("\\bDelivery\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) && it.length <= 32 }) {
            return OfferService.DELIVERY
        }
        if (items.containsMatchIn(text)) return OfferService.SHOP_AND_PAY
        return OfferService.UNKNOWN
    }

    fun hasAnchors(text: String): Boolean {
        val lines = lines(text)
        val hasAction = lines.any { it.equals("Accept", true) || it.equals("Match", true) }
        val hasMoney = lines.any { money.containsMatchIn(it) }
        return hasAction && hasMoney && (lines.any { header.matches(it) } || lines.any { route.containsMatchIn(it) })
    }

    fun read(text: String): UberCard? {
        val all = lines(text)
        if (all.isEmpty()) return null
        val end = all.indexOfLast { action.containsMatchIn(it) && !it.startsWith("decline", true) }
            .takeIf { it >= 0 } ?: all.lastIndex
        val slice = extractSlice(all, end)
        if (slice.none { money.containsMatchIn(it) }) return null

        val cardText = slice.joinToString("\n")
        val serviceKind = kind(cardText)
        val channel = when {
            slice.any { it.contains("Trip Radar", true) || it.equals("Match", true) || it.startsWith("Match ", true) } ->
                OfferChannel.TRIP_RADAR
            slice.any { it.contains("Exclusive", true) || it.equals("Accept", true) || it.startsWith("Accept ", true) } ->
                OfferChannel.EXCLUSIVE
            else -> OfferChannel.UNKNOWN
        }
        val isPreferred = preferred.containsMatchIn(cardText)
        val hasTip = if (tipHint.containsMatchIn(cardText)) true else null
        val itemCount = items.find(cardText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..500 }

        val (merchant, pickup, destination) = extractLocations(slice, serviceKind)
        return UberCard(serviceKind, channel, isPreferred, hasTip, itemCount, merchant, pickup, destination, cardText)
    }

    private fun extractSlice(all: List<String>, end: Int): List<String> {
        val headerIdx = all.indices.lastOrNull {
            it < end && (header.containsMatchIn(all[it]) || all[it].contains("Trip Radar", true))
        }
        if (headerIdx != null) return all.subList(headerIdx, end + 1)
        val priceIdx = all.indices.lastOrNull { it < end && money.containsMatchIn(all[it]) } ?: return all.subList(0, end + 1)
        val start = (priceIdx - 2).coerceAtLeast(0)
        return all.subList(start, end + 1)
    }

    private fun extractLocations(slice: List<String>, kind: OfferService): Triple<String?, String?, String?> {
        val priceIdx = slice.indexOfFirst { money.containsMatchIn(it) }
        val startIdx = if (priceIdx >= 0) priceIdx + 1 else 0
        val candidates = slice.subList(startIdx, slice.size).filter { line ->
            line.length in 4..200 &&
                !action.containsMatchIn(line) &&
                !decoration.matches(line) &&
                !money.containsMatchIn(line) &&
                !route.containsMatchIn(line) &&
                !tipHint.containsMatchIn(line) &&
                !shopHint.containsMatchIn(line)
        }
        if (candidates.isEmpty()) return Triple(null, null, null)
        val first = mutableListOf(candidates.first())
        var nextIdx = 1
        if (kind == OfferService.PACKAGE && nextIdx < candidates.size && postal.matches(candidates[nextIdx])) {
            first += candidates[nextIdx++]
        }
        val origin = first.joinToString(" ")
        val dest = candidates.drop(nextIdx).lastOrNull()
        return if (kind == OfferService.PACKAGE) Triple(null, origin, dest) else Triple(origin, null, dest)
    }

    private fun lines(text: String) = text.lines().map {
        it.trim().replace(Regex("\\s+"), " ")
            .replace(Regex("^(Package|Delivery)(\\s+Exclusive)?\\s+[×✕x]$", RegexOption.IGNORE_CASE), "$1$2")
    }.filter(String::isNotBlank)
}
