package io.github.pennymorant.offerpilot.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

internal fun buildOverlayViews(
    context: Context,
    onPause: () -> Unit,
    onDecline: () -> Unit,
): OverlayViews {
    val d = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()
    val cardBg = GradientDrawable().apply { setColor(Color.BLACK) }
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        clipToOutline = true
        background = cardBg
        elevation = dp(4).toFloat()
    }
    fun pill(size: Float) = TextView(context).apply {
        textSize = size; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
        setPadding(dp(8), dp(3), dp(8), dp(3))
        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat() }
    }
    fun tile() = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(6), dp(5), dp(6), dp(5))
        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) }
    }
    fun actionPill(onClick: () -> Unit) = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = GradientDrawable().apply { cornerRadius = dp(12).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
        setOnClickListener { onClick() }
    }

    val leftBadge = pill(11f)
    val rightBadge = pill(11f).apply { gravity = Gravity.END }
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        addView(leftBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(rightBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    val amountText = TextView(context).apply {
        textSize = 24f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
    }
    val heroRouteText = TextView(context).apply {
        textSize = 15.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1; gravity = Gravity.END
    }
    val heroRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(5))
        addView(amountText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(heroRouteText)
    }

    val perMileTile = tile().apply { (layoutParams as LinearLayout.LayoutParams).marginStart = 0 }
    val hourlyTile = tile()
    val verdictTile = tile()
    val metricsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 0, 0, dp(6))
        addView(perMileTile); addView(hourlyTile); addView(verdictTile)
    }

    val tagsText = TextView(context).apply {
        textSize = 11f; typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(3))
    }
    val routeText = TextView(context).apply {
        textSize = 11.5f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(5))
    }
    val verdictText = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    val extraText = TextView(context).apply {
        textSize = 10.5f; fontFeatureSettings = "tnum"; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(2), 0, 0)
    }
    val infoBox = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(7), dp(10), dp(7))
        background = GradientDrawable().apply { cornerRadius = dp(12).toFloat() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(6)
        }
        addView(verdictText); addView(extraText)
    }

    val pauseBtn = actionPill(onPause).apply { text = "暂停自动" }
    val declineBtn = actionPill(onDecline).apply { text = "秒拒"; visibility = View.GONE }
    val switchBtn = actionPill {}.apply {
        visibility = View.GONE; (layoutParams as LinearLayout.LayoutParams).marginEnd = 0
    }
    val actionsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; addView(pauseBtn); addView(declineBtn); addView(switchBtn)
    }

    root.addView(topRow); root.addView(heroRow); root.addView(metricsRow); root.addView(tagsText)
    root.addView(routeText); root.addView(infoBox); root.addView(actionsRow)
    return OverlayViews(
        root, cardBg, topRow, leftBadge, rightBadge, heroRow, amountText, heroRouteText,
        metricsRow, perMileTile, hourlyTile, verdictTile, tagsText, routeText, infoBox, verdictText, extraText,
        actionsRow, pauseBtn, declineBtn, switchBtn,
    )
}
