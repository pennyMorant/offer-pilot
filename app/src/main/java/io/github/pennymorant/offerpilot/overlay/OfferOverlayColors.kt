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

internal fun applyIslandColors(v: OverlayViews, colors: ColorScheme, alert: Boolean, eligible: Boolean, strokePx: Int) {
    v.cardBg.setColor(Color.BLACK)
    v.cardBg.setStroke(0, Color.TRANSPARENT)
    val fgColor = Color.WHITE
    v.amountText.setTextColor(fgColor)
    v.heroRouteText.setTextColor(Color.rgb(212, 218, 226))
    v.tagsText.setTextColor(colors.tertiary.toArgb())
    v.routeText.setTextColor(Color.rgb(196, 203, 214))
    (v.infoBox.background as? GradientDrawable)?.setColor(Color.rgb(22, 25, 31))
    v.verdictText.setTextColor(if (eligible) colors.primary.toArgb() else Color.rgb(212, 218, 226))
    v.extraText.setTextColor(Color.rgb(156, 163, 175))
    val tileBg = Color.rgb(22, 25, 31)
    v.perMileTile.stylePill(tileBg, fgColor)
    v.hourlyTile.stylePill(tileBg, fgColor)
    v.pauseBtn.stylePill(colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb())
    v.declineBtn.stylePill(Color.rgb(42, 46, 54), Color.WHITE)
    v.switchBtn.stylePill(colors.tertiaryContainer.toArgb(), colors.onTertiaryContainer.toArgb())
    v.closeBtn.stylePill(Color.rgb(34, 38, 46), Color.rgb(210, 216, 224))
}
