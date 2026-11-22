package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.model.TextRegion

/** Geometrically isolated offer card ROI from screen nodes or OCR boxes. */
data class LocatedOfferCard(
    val regions: List<TextRegion>,
    val amountRegion: TextRegion?,
    val actionRegion: TextRegion?,
    val fullText: String,
    val cardText: String,
)

object OfferCardLocator {
    private val money = Regex("(?<![\\p{L}\\d+−-])(?:US\\$|\\$)\\s*\\d{1,4}(?:,\\d{3})*(?:\\.\\d{2})?(?:\\+)?(?![\\d.,])")
    private val surge = Regex("^\\+\\s*(?:US\\$|\\$)")
    private val uberActions = setOf("accept", "match", "接受", "接单")
    private val ddActions = setOf("accept", "接受", "接单")

    fun locate(platform: Platform, rawRegions: List<TextRegion>): LocatedOfferCard {
        val valid = rawRegions.filter(TextRegion::valid)
        if (valid.isEmpty()) return LocatedOfferCard(emptyList(), null, null, "", "")
        val fullText = SpatialPageText.assemble(valid)
        val minTop = valid.minOf { it.top }
        val maxBottom = valid.maxOf { it.bottom }
        val minLeft = valid.minOf { it.left }
        val maxRight = valid.maxOf { it.right }
        val span = (maxBottom - minTop).coerceAtLeast(1)
        val width = (maxRight - minLeft).coerceAtLeast(1)

        val actions = if (platform == Platform.UBER_EATS) uberActions else ddActions
        val action = findAction(valid, actions, minTop, span)
        val amount = findMainAmount(valid, action, minTop, span)

        if (amount == null && action == null) {
            return LocatedOfferCard(valid, null, null, fullText, fullText)
        }

        val cardBottom = ((action?.bottom ?: maxBottom) + (span * 0.025).toInt()).coerceAtMost(maxBottom)
        val cardTop = if (amount != null) {
            // Only one badge row (Delivery/Package + Exclusive + X) sits above the payout amount.
            val headerMargin = maxOf((amount.height * 2.0).toInt(), (span * 0.08).toInt())
            val candidateTop = (amount.top - headerMargin).coerceAtLeast(minTop)
            // Keep entire frame when user imported an already-cropped offer card screenshot.
            if (amount.top - minTop <= span * 0.30) minTop else candidateTop
        } else {
            (minTop + (span * 0.48).toInt()).coerceAtMost(action!!.top)
        }

        val cardRegions = valid.filter { region ->
            val inY = region.centerY in cardTop..cardBottom
            val trimmed = region.text.trim()
            val edgeNoise = trimmed.length <= 3 && !trimmed.contains('$') &&
                !trimmed.equals("x", true) && trimmed != "×" && trimmed != "✕" &&
                (region.left <= minLeft + (width * 0.03).toInt() || region.right >= maxRight - (width * 0.03).toInt())
            inY && !edgeNoise
        }
        val cardText = SpatialPageText.assemble(cardRegions)
        return LocatedOfferCard(cardRegions, amount, action, fullText, cardText)
    }

    private fun findAction(regions: List<TextRegion>, actions: Set<String>, minTop: Int, span: Int): TextRegion? {
        val matches = regions.filter { region ->
            val norm = region.text.trim().lowercase(java.util.Locale.ROOT)
            actions.any { act -> norm == act || norm.startsWith("$act ") }
        }
        if (matches.isEmpty()) return null
        val lower = matches.filter { it.centerY >= minTop + span * 0.40 }
        return (lower.ifEmpty { matches }).maxByOrNull { it.centerY }
    }

    private fun findMainAmount(regions: List<TextRegion>, action: TextRegion?, minTop: Int, span: Int): TextRegion? {
        val upperLimit = action?.bottom ?: (minTop + span)
        val candidates = regions.filter { region ->
            val trimmed = region.text.trim()
            region.top <= upperLimit &&
                !surge.containsMatchIn(trimmed) &&
                !trimmed.contains('-') && !trimmed.contains('−') &&
                money.containsMatchIn(trimmed)
        }
        if (candidates.isEmpty()) return null
        // Exclude top status/earnings header bar when lower card candidates exist.
        val body = candidates.filter { it.centerY >= minTop + span * 0.18 }
        val pool = body.ifEmpty { candidates }
        // Offer payout is rendered in the largest bold font inside the card.
        val maxHeight = pool.maxOf { it.height }
        val largest = pool.filter { it.height * 10 >= maxHeight * 8 }
        return if (action != null) {
            largest.minByOrNull { kotlin.math.abs(action.top - it.bottom) }
        } else {
            largest.maxByOrNull { it.height }
        }
    }
}
