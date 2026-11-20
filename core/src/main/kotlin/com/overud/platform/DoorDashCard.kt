package com.overud.platform

import com.overud.model.OfferChannel
import com.overud.model.OfferService

/** Flexible DoorDash Dasher offer card extractor (Delivery, High Paying, Shop & Deliver, Stacked). */
internal data class DoorDashCardData(
    val kind: OfferService,
    val channel: OfferChannel,
    val preferredTag: Boolean,
    val includesTip: Boolean?,
    val itemCount: Int?,
    val merchant: String?,
    val destination: String?,
)

internal object DoorDashCard {
    private val money = Regex("(?:US\\$|\\$)\\s*\\d{1,4}(?:,\\d{3})*(?:\\.\\d{2})?")
    private val route = Regex("\\d+(?:\\.\\d+)?\\s*(?:mi(?:les?)?|km)\\b|\\d+\\s*min(?:ute)?s?\\b", RegexOption.IGNORE_CASE)
    private val action = Regex("^(?:accept|decline|接受|接单|拒绝)(?:\\s|$)", RegexOption.IGNORE_CASE)
    private val shopHint = Regex("shop\\s*(?:&|and)\\s*deliver|red\\s+card|商超|采购", RegexOption.IGNORE_CASE)
    private val packageHint = Regex("dashlink|package|parcel|retail\\s+pickup", RegexOption.IGNORE_CASE)
    private val preferred = Regex("high[-\\s]paying|your\\s+(?:platinum|gold|silver)\\s+status|top\\s+dasher|优选", RegexOption.IGNORE_CASE)
    private val tipHint = Regex("incl\\.?\\s*tips?|total\\s+will\\s+be\\s+higher|guaranteed|customer\\s+tip|含小费", RegexOption.IGNORE_CASE)
    private val items = Regex("\\b(\\d{1,3})\\s*(?:items?|units?|件)\\b", RegexOption.IGNORE_CASE)
    private val deliverBy = Regex("^deliver\\s+by\\b|^pick\\s*up\\s+by\\b", RegexOption.IGNORE_CASE)
    private val decoration = Regex(
        "^(?:Decline|Accept|New\\s+Order|Guaranteed.*|Total\\s+will\\s+be\\s+higher.*|Includes.*tip.*|" +
            "Peak\\s+Pay.*|Shop\\s*&\\s*Deliver.*|Red\\s+Card.*|High[-\\s]Paying.*|DashLink.*|" +
            "\\d+\\s*(?:items?|units?)|\\d+\\s*s|[×✕x])$",
        RegexOption.IGNORE_CASE,
    )

    fun read(cardText: String): DoorDashCardData {
        val lines = cardText.lines().map { it.trim().replace(Regex("\\s+"), " ") }.filter(String::isNotBlank)
        val kind = when {
            shopHint.containsMatchIn(cardText) || items.containsMatchIn(cardText) -> OfferService.SHOP_AND_PAY
            packageHint.containsMatchIn(cardText) -> OfferService.PACKAGE
            lines.isNotEmpty() -> OfferService.DELIVERY
            else -> OfferService.UNKNOWN
        }
        val isPreferred = preferred.containsMatchIn(cardText)
        val hasTip = if (tipHint.containsMatchIn(cardText)) true else null
        val itemCount = items.find(cardText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..500 }

        val candidates = lines.filter { line ->
            line.length in 3..200 &&
                !action.containsMatchIn(line) &&
                !decoration.matches(line) &&
                !money.containsMatchIn(line) &&
                !route.containsMatchIn(line) &&
                !deliverBy.containsMatchIn(line) &&
                !tipHint.containsMatchIn(line) &&
                !preferred.containsMatchIn(line) &&
                !shopHint.containsMatchIn(line)
        }
        val merchant = candidates.firstOrNull()
        val destination = if (candidates.size >= 2) candidates.last() else null
        return DoorDashCardData(kind, OfferChannel.EXCLUSIVE, isPreferred, hasTip, itemCount, merchant, destination)
    }
}
