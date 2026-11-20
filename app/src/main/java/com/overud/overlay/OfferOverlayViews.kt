package com.overud.overlay

import android.content.Context
import android.graphics.Color
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
    val topRow: LinearLayout,
    val leftBadge: TextView,
    val cameraGap: View,
    val rightBadge: TextView,
    val heroRow: LinearLayout,
    val amountText: TextView,
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
    cutoutWidthPx: Int,
    cutoutHeightPx: Int,
    onPause: () -> Unit,
    onDecline: () -> Unit,
): OverlayViews {
    val d = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()
    val cardBg = GradientDrawable().apply { cornerRadius = dp(24).toFloat() }
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(6), dp(14), dp(8))
        background = cardBg
        elevation = dp(12).toFloat()
    }
    fun pill(size: Float) = TextView(context).apply {
        textSize = size; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
        setPadding(dp(7), dp(2), dp(7), dp(2))
        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat() }
    }
    fun tile() = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(6), dp(4), dp(6), dp(4))
        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) }
    }
    fun actionPill(onClick: () -> Unit) = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(10), dp(5), dp(10), dp(5))
        background = GradientDrawable().apply { cornerRadius = dp(12).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
        setOnClickListener { onClick() }
    }

    val leftBadge = pill(11f)
    val cameraGap = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(cutoutWidthPx.coerceAtLeast(dp(8)), cutoutHeightPx.coerceAtLeast(dp(20)))
    }
    val rightBadge = pill(11f).apply { gravity = Gravity.END }
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(leftBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(cameraGap)
        addView(rightBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    val amountText = TextView(context).apply {
        textSize = 22f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
    }
    val perMileTile = tile()
    val hourlyTile = tile()
    val verdictTile = tile()
    val heroRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(5))
        addView(amountText)
        addView(perMileTile); addView(hourlyTile); addView(verdictTile)
    }

    val detailText = TextView(context).apply {
        textSize = 11.5f; fontFeatureSettings = "tnum"; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(1), 0, dp(6))
    }

    val pauseBtn = actionPill(onPause).apply { text = "暂停自动" }
    val declineBtn = actionPill(onDecline).apply { text = "秒拒"; visibility = View.GONE }
    val switchBtn = actionPill {}.apply {
        visibility = View.GONE; (layoutParams as LinearLayout.LayoutParams).marginEnd = 0
    }
    val actionsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; addView(pauseBtn); addView(declineBtn); addView(switchBtn)
    }

    root.addView(topRow); root.addView(heroRow); root.addView(detailText); root.addView(actionsRow)
    return OverlayViews(
        root, cardBg, topRow, leftBadge, cameraGap, rightBadge, heroRow, amountText,
        perMileTile, hourlyTile, verdictTile, detailText, actionsRow, pauseBtn, declineBtn, switchBtn,
    )
}

internal fun TextView.stylePill(bg: Int, fg: Int) {
    (background as? GradientDrawable)?.setColor(bg)
    setTextColor(fg)
}

internal fun applyIslandColors(v: OverlayViews, colors: ColorScheme, alert: Boolean, strokePx: Int) {
    v.cardBg.setColor(Color.BLACK)
    v.cardBg.setStroke(if (alert) strokePx * 2 else 0, if (alert) colors.error.toArgb() else Color.TRANSPARENT)
    val fgColor = Color.WHITE
    val subColor = if (alert) colors.error.toArgb() else Color.rgb(176, 182, 190)
    v.amountText.setTextColor(if (alert) colors.error.toArgb() else fgColor)
    v.detailText.setTextColor(subColor)
    val tileBg = Color.rgb(22, 24, 28)
    v.perMileTile.stylePill(tileBg, fgColor)
    v.hourlyTile.stylePill(tileBg, fgColor)
    v.pauseBtn.stylePill(colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb())
    v.declineBtn.stylePill(colors.errorContainer.toArgb(), colors.onErrorContainer.toArgb())
    v.switchBtn.stylePill(colors.tertiaryContainer.toArgb(), colors.onTertiaryContainer.toArgb())
}
