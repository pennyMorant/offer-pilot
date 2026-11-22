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
    val outline = colors.outlineVariant.toArgb()
    val softStroke = Color.argb(115, Color.red(outline), Color.green(outline), Color.blue(outline))
    v.cardBg.setColor(colors.surfaceContainerHigh.toArgb())
    v.cardBg.setStroke(strokePx, softStroke)

    val onSurface = colors.onSurface.toArgb()
    val onVariant = colors.onSurfaceVariant.toArgb()
    val tileBg = if (dark) colors.surfaceContainer.toArgb() else colors.surfaceContainerLowest.toArgb()

    v.amountText.setTextColor(onSurface)
    v.heroRouteText.setTextColor(onVariant)
    v.tagsText.setTextColor(colors.tertiary.toArgb())
    v.routeText.setTextColor(onVariant)
    (v.perMileBox.background as? GradientDrawable)?.setColor(tileBg)
    (v.hourlyBox.background as? GradientDrawable)?.setColor(tileBg)
    v.perMileLabel.setTextColor(onVariant)
    v.hourlyLabel.setTextColor(onVariant)
    v.perMileTile.setTextColor(onSurface)
    v.hourlyTile.setTextColor(onSurface)

    (v.infoBox.background as? GradientDrawable)?.setColor(colors.surfaceContainer.toArgb())
    v.verdictText.setTextColor(if (eligible) colors.primary.toArgb() else onSurface)
    v.extraText.setTextColor(onVariant)

    v.closeBtn.stylePill(colors.surfaceContainerHighest.toArgb(), onVariant)
    v.pauseBtn.stylePill(colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb())
    v.declineBtn.stylePill(colors.secondaryContainer.toArgb(), colors.onSecondaryContainer.toArgb())
    v.switchBtn.stylePill(colors.tertiaryContainer.toArgb(), colors.onTertiaryContainer.toArgb())
}
