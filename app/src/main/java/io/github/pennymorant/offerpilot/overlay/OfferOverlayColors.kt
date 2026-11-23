package io.github.pennymorant.offerpilot.overlay

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

internal fun TextView.stylePill(bg: Int, fg: Int) {
    (background as? GradientDrawable)?.setColor(bg)
    setTextColor(fg)
}

internal fun applyExpressiveColors(v: OverlayViews, colors: ColorScheme, dark: Boolean, eligible: Boolean, strokePx: Int) {
    val cardColor = if (eligible) colors.primaryContainer.toArgb() else colors.surfaceContainerHigh.toArgb()
    val outline = colors.outlineVariant.toArgb()
    val softStroke = Color.argb(110, Color.red(outline), Color.green(outline), Color.blue(outline))
    v.cardBg.setColor(cardColor)
    v.cardBg.setStroke(if (eligible) 0 else strokePx, if (eligible) Color.TRANSPARENT else softStroke)

    val onCard = if (eligible) colors.onPrimaryContainer.toArgb() else colors.onSurface.toArgb()
    val onVariant = if (eligible) Color.argb(200, Color.red(onCard), Color.green(onCard), Color.blue(onCard))
        else colors.onSurfaceVariant.toArgb()
    val tileBg = if (dark) colors.surfaceContainer.toArgb() else colors.surfaceContainerLowest.toArgb()
    val onTile = colors.onSurface.toArgb()
    val onTileLabel = colors.onSurfaceVariant.toArgb()

    v.verdictBadge.stylePill(
        if (eligible) colors.primary.toArgb() else colors.surfaceContainerHighest.toArgb(),
        if (eligible) colors.onPrimary.toArgb() else colors.onSurfaceVariant.toArgb(),
    )
    v.scoreBadge.stylePill(
        if (eligible) tileBg else colors.surfaceContainerHighest.toArgb(),
        if (eligible) colors.primary.toArgb() else colors.onSurfaceVariant.toArgb(),
    )
    v.closeBtn.stylePill(
        if (eligible) tileBg else colors.surfaceContainerHighest.toArgb(),
        colors.onSurfaceVariant.toArgb(),
    )
    v.reasonText.setTextColor(onCard)

    listOf(v.amountBox, v.perMileBox, v.hourlyBox).forEach {
        (it.background as? GradientDrawable)?.setColor(tileBg)
    }
    listOf(v.amountLabel, v.perMileLabel, v.hourlyLabel).forEach { it.setTextColor(onTileLabel) }
    listOf(v.amountValue, v.perMileValue, v.hourlyValue).forEach { it.setTextColor(onTile) }
    v.routeLine.setTextColor(onVariant)
}
