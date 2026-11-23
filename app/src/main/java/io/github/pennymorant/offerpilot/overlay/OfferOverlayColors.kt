package io.github.pennymorant.offerpilot.overlay

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import io.github.pennymorant.offerpilot.model.Decision

internal enum class OfferTier(val label: String) {
    EXCELLENT("★ 必接"),
    GOOD("✓ 建议接"),
    BORDERLINE("△ 酌情"),
    SKIP("✕ 跳过"),
}

internal fun resolveOfferTier(decision: Decision?): OfferTier {
    val eligible = decision?.eligible == true
    val score = decision?.valuation?.score
    return when {
        eligible && (score ?: 0) >= 85 -> OfferTier.EXCELLENT
        eligible -> OfferTier.GOOD
        (score ?: 0) >= 55 -> OfferTier.BORDERLINE
        else -> OfferTier.SKIP
    }
}

private data class TierPalette(
    val cardBg: Int,
    val onCard: Int,
    val onCardMuted: Int,
    val badgeBg: Int,
    val onBadge: Int,
    val chipBg: Int,
    val onChip: Int,
    val tileBg: Int,
    val onTile: Int,
    val onTileLabel: Int,
)

private fun paletteFor(tier: OfferTier, dark: Boolean): TierPalette = when (tier) {
    OfferTier.EXCELLENT -> if (dark) TierPalette(
        cardBg = 0xFF157A35.toInt(), onCard = Color.WHITE, onCardMuted = 0xD9FFFFFF.toInt(),
        badgeBg = 0xFFC4EED0.toInt(), onBadge = 0xFF072711.toInt(),
        chipBg = 0xFF0D5925.toInt(), onChip = 0xFFE6F8EB.toInt(),
        tileBg = 0xFF0D5925.toInt(), onTile = Color.WHITE, onTileLabel = 0xBFE6F8EB.toInt(),
    ) else TierPalette(
        cardBg = 0xFF146C2E.toInt(), onCard = Color.WHITE, onCardMuted = 0xD9FFFFFF.toInt(),
        badgeBg = Color.WHITE, onBadge = 0xFF0D5222.toInt(),
        chipBg = 0xFF0F5323.toInt(), onChip = Color.WHITE,
        tileBg = 0xFF0F5323.toInt(), onTile = Color.WHITE, onTileLabel = 0xCCFFFFFF.toInt(),
    )

    OfferTier.GOOD -> if (dark) TierPalette(
        cardBg = 0xFF1B3B2B.toInt(), onCard = 0xFFE2F5E9.toInt(), onCardMuted = 0xFFB0D6BF.toInt(),
        badgeBg = 0xFF6DD58C.toInt(), onBadge = 0xFF072711.toInt(),
        chipBg = 0xFF132C20.toInt(), onChip = 0xFFC4EED0.toInt(),
        tileBg = 0xFF132C20.toInt(), onTile = 0xFFF2FBF5.toInt(), onTileLabel = 0xFF9AC4AA.toInt(),
    ) else TierPalette(
        cardBg = 0xFFC4EED0.toInt(), onCard = 0xFF072711.toInt(), onCardMuted = 0xFF1F4E2E.toInt(),
        badgeBg = 0xFF146C2E.toInt(), onBadge = Color.WHITE,
        chipBg = 0xFFF2FBF5.toInt(), onChip = 0xFF146C2E.toInt(),
        tileBg = 0xFFF2FBF5.toInt(), onTile = 0xFF072711.toInt(), onTileLabel = 0xFF386646.toInt(),
    )

    OfferTier.BORDERLINE -> if (dark) TierPalette(
        cardBg = 0xFF5C3D00.toInt(), onCard = 0xFFFFE6A3.toInt(), onCardMuted = 0xFFE5C470.toInt(),
        badgeBg = 0xFFFFB74D.toInt(), onBadge = 0xFF382200.toInt(),
        chipBg = 0xFF422B00.toInt(), onChip = 0xFFFFE082.toInt(),
        tileBg = 0xFF422B00.toInt(), onTile = 0xFFFFF3D1.toInt(), onTileLabel = 0xFFD9B86C.toInt(),
    ) else TierPalette(
        cardBg = 0xFFFFE082.toInt(), onCard = 0xFF3B2400.toInt(), onCardMuted = 0xFF664200.toInt(),
        badgeBg = 0xFF8C5000.toInt(), onBadge = Color.WHITE,
        chipBg = 0xFFFFF8E1.toInt(), onChip = 0xFF7A4400.toInt(),
        tileBg = 0xFFFFF8E1.toInt(), onTile = 0xFF2B1900.toInt(), onTileLabel = 0xFF6E4B16.toInt(),
    )

    OfferTier.SKIP -> if (dark) TierPalette(
        cardBg = 0xFF8C1D18.toInt(), onCard = Color.WHITE, onCardMuted = 0xE0FFDAD6.toInt(),
        badgeBg = 0xFFFFB4AB.toInt(), onBadge = 0xFF56110D.toInt(),
        chipBg = 0xFF69120E.toInt(), onChip = 0xFFFFDAD6.toInt(),
        tileBg = 0xFF69120E.toInt(), onTile = Color.WHITE, onTileLabel = 0xCCFFDAD6.toInt(),
    ) else TierPalette(
        cardBg = 0xFFB3261E.toInt(), onCard = Color.WHITE, onCardMuted = 0xD9FFFFFF.toInt(),
        badgeBg = Color.WHITE, onBadge = 0xFF8C1D18.toInt(),
        chipBg = 0xFF8C1D18.toInt(), onChip = 0xFFFFDAD6.toInt(),
        tileBg = 0xFF8C1D18.toInt(), onTile = Color.WHITE, onTileLabel = 0xCCFFDAD6.toInt(),
    )
}

internal fun TextView.stylePill(bg: Int, fg: Int) {
    (background as? GradientDrawable)?.setColor(bg)
    setTextColor(fg)
}

internal fun applyTierColors(v: OverlayViews, tier: OfferTier, dark: Boolean) {
    val p = paletteFor(tier, dark)
    v.cardBg.setColor(p.cardBg)
    v.cardBg.setStroke(0, Color.TRANSPARENT)

    v.verdictBadge.stylePill(p.badgeBg, p.onBadge)
    v.scoreBadge.stylePill(p.chipBg, p.onChip)
    v.closeBtn.stylePill(p.chipBg, p.onChip)
    v.reasonText.setTextColor(p.onCard)

    listOf(v.amountBox, v.perMileBox, v.hourlyBox).forEach {
        (it.background as? GradientDrawable)?.setColor(p.tileBg)
    }
    listOf(v.amountLabel, v.perMileLabel, v.hourlyLabel).forEach { it.setTextColor(p.onTileLabel) }
    listOf(v.amountValue, v.perMileValue, v.hourlyValue).forEach { it.setTextColor(p.onTile) }
    v.routeLine.setTextColor(p.onCardMuted)
}
