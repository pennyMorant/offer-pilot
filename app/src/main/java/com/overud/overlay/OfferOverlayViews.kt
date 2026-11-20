package com.overud.overlay

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

internal class OverlayViews(
    val root: LinearLayout,
    val cardBg: GradientDrawable,
    val headerRow: LinearLayout,
    val modeBadge: TextView,
    val scoreBadge: TextView,
    val heroRow: LinearLayout,
    val amountText: TextView,
    val routeText: TextView,
    val metricsRow: LinearLayout,
    val perMileTile: TextView,
    val hourlyTile: TextView,
    val verdictTile: TextView,
    val detailText: TextView,
    val actionsRow: LinearLayout,
    val pauseBtn: TextView,
    val declineBtn: TextView,
    val switchBtn: TextView,
)

internal fun buildOverlayViews(
    context: Context,
    onPause: () -> Unit,
    onDecline: () -> Unit,
): OverlayViews {
    val d = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()
    val cardBg = GradientDrawable().apply { cornerRadius = dp(16).toFloat() }
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(7), dp(10), dp(7))
        background = cardBg
        elevation = dp(8).toFloat()
    }
    fun pill(size: Float) = TextView(context).apply {
        textSize = size; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        setPadding(dp(6), dp(2), dp(6), dp(2))
        background = GradientDrawable().apply { cornerRadius = dp(8).toFloat() }
    }
    fun tile() = TextView(context).apply {
        textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(4), dp(3), dp(4), dp(3))
        background = GradientDrawable().apply { cornerRadius = dp(8).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(3) }
    }
    fun actionPill(onClick: () -> Unit) = TextView(context).apply {
        textSize = 11f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(8), dp(4), dp(8), dp(4))
        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(3) }
        setOnClickListener { onClick() }
    }

    val modeBadge = pill(10f)
    val scoreBadge = pill(10f).apply { visibility = View.GONE }
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(modeBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
        addView(scoreBadge)
    }

    val amountText = TextView(context).apply {
        textSize = 19f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
    }
    val routeText = TextView(context).apply {
        textSize = 11f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; gravity = Gravity.END
    }
    val heroRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(3), 0, dp(3))
        addView(amountText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(routeText)
    }

    val perMileTile = tile()
    val hourlyTile = tile()
    val verdictTile = tile().apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = 0 }
    val metricsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(1), 0, dp(4))
        addView(perMileTile); addView(hourlyTile); addView(verdictTile)
    }

    val detailText = TextView(context).apply {
        textSize = 10.5f; fontFeatureSettings = "tnum"; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(1), 0, dp(4))
    }

    val pauseBtn = actionPill(onPause).apply { text = "暂停自动" }
    val declineBtn = actionPill(onDecline).apply { text = "秒拒"; visibility = View.GONE }
    val switchBtn = actionPill {}.apply {
        visibility = View.GONE; (layoutParams as LinearLayout.LayoutParams).marginEnd = 0
    }
    val actionsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; addView(pauseBtn); addView(declineBtn); addView(switchBtn)
    }

    root.addView(headerRow); root.addView(heroRow); root.addView(metricsRow)
    root.addView(detailText); root.addView(actionsRow)
    return OverlayViews(
        root, cardBg, headerRow, modeBadge, scoreBadge, heroRow, amountText, routeText,
        metricsRow, perMileTile, hourlyTile, verdictTile, detailText, actionsRow, pauseBtn, declineBtn, switchBtn,
    )
}

internal fun TextView.stylePill(bg: Int, fg: Int) {
    (background as? GradientDrawable)?.setColor(bg)
    setTextColor(fg)
}

internal fun applyBaseColors(v: OverlayViews, colors: ColorScheme, alert: Boolean, strokePx: Int) {
    val bgColor = if (alert) colors.errorContainer.toArgb() else colors.surfaceContainer.toArgb()
    val fgColor = if (alert) colors.onErrorContainer.toArgb() else colors.onSurface.toArgb()
    v.cardBg.setColor(bgColor)
    v.cardBg.setStroke(strokePx, (if (alert) colors.error else colors.outlineVariant).toArgb())
    v.amountText.setTextColor(fgColor)
    v.routeText.setTextColor(if (alert) fgColor else colors.onSurfaceVariant.toArgb())
    v.detailText.setTextColor(fgColor)
    val tileBg = if (alert) colors.surface.toArgb() else colors.surfaceContainerHigh.toArgb()
    val tileFg = colors.onSurface.toArgb()
    v.perMileTile.stylePill(tileBg, tileFg)
    v.hourlyTile.stylePill(tileBg, tileFg)
    v.pauseBtn.stylePill(colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb())
    v.declineBtn.stylePill(colors.errorContainer.toArgb(), colors.onErrorContainer.toArgb())
    v.switchBtn.stylePill(colors.tertiaryContainer.toArgb(), colors.onTertiaryContainer.toArgb())
}
