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
    v.cardBg.setStroke(if (alert) strokePx * 2 else 0, if (alert) colors.error.toArgb() else Color.TRANSPARENT)
    val fgColor = Color.WHITE
    v.amountText.setTextColor(if (alert) colors.error.toArgb() else fgColor)
    v.heroRouteText.setTextColor(if (alert) colors.error.toArgb() else Color.rgb(232, 236, 242))
    v.tagsText.setTextColor(if (alert) colors.error.toArgb() else colors.tertiary.toArgb())
    v.routeText.setTextColor(Color.rgb(228, 232, 238))
    (v.infoBox.background as? GradientDrawable)?.setColor(Color.rgb(18, 20, 24))
    v.verdictText.setTextColor(when {
        alert -> colors.error.toArgb()
        eligible -> colors.primary.toArgb()
        else -> Color.rgb(245, 190, 120)
    })
    v.extraText.setTextColor(Color.rgb(162, 168, 178))
    val tileBg = Color.rgb(22, 24, 28)
    v.perMileTile.stylePill(tileBg, fgColor)
    v.hourlyTile.stylePill(tileBg, fgColor)
    v.pauseBtn.stylePill(colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb())
    v.declineBtn.stylePill(colors.errorContainer.toArgb(), colors.onErrorContainer.toArgb())
    v.switchBtn.stylePill(colors.tertiaryContainer.toArgb(), colors.onTertiaryContainer.toArgb())
}
