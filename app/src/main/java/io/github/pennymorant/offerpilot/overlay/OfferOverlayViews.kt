package io.github.pennymorant.offerpilot.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.pennymorant.offerpilot.R

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
        setPadding(dp(14), dp(6), dp(14), dp(6))
        clipToOutline = true
        background = cardBg
        elevation = dp(6).toFloat()
    }
    fun pill(size: Float) = TextView(context).apply {
        textSize = size; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(9), dp(4), dp(9), dp(4))
        background = GradientDrawable().apply { cornerRadius = dp(999).toFloat() }
    }
    fun tile() = TextView(context).apply {
        textSize = 12f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(8), dp(7), dp(8), dp(7))
        background = GradientDrawable().apply { cornerRadius = dp(14).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) }
    }
    fun actionPill(onClick: () -> Unit) = TextView(context).apply {
        textSize = 12f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; maxLines = 1
        setPadding(dp(12), dp(9), dp(12), dp(9))
        background = GradientDrawable().apply { cornerRadius = dp(999).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
        setOnClickListener { onClick() }
    }

    val statusDot = View(context).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(74, 222, 128)) }
    }
    val statusHalo = FrameLayout(context).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(64, 74, 222, 128)) }
        addView(statusDot, FrameLayout.LayoutParams(dp(9), dp(9), Gravity.CENTER))
    }
    val leftBadge = pill(11.5f)
    val cutoutSpacer = View(context).apply { visibility = View.GONE }
    val rightBadge = pill(11.5f).apply { gravity = Gravity.END }
    val appLogo = ImageView(context).apply {
        setImageResource(R.mipmap.ic_launcher_round)
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    val closeBtn = TextView(context).apply {
        text = "✕"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        visibility = View.GONE
    }
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(statusHalo, LinearLayout.LayoutParams(dp(18), dp(18)))
        addView(leftBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(cutoutSpacer, LinearLayout.LayoutParams(0, 1))
        addView(rightBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(appLogo, LinearLayout.LayoutParams(dp(20), dp(20)))
        addView(closeBtn, LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginStart = dp(8) })
    }

    val amountText = TextView(context).apply {
        textSize = 28f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
    }
    val heroRouteText = TextView(context).apply {
        textSize = 15f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1; gravity = Gravity.END
    }
    val heroRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(8), 0, dp(8))
        addView(amountText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(heroRouteText)
    }

    val perMileTile = tile().apply { (layoutParams as LinearLayout.LayoutParams).marginStart = 0 }
    val hourlyTile = tile()
    val verdictTile = tile()
    val metricsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 0, 0, dp(8))
        addView(perMileTile); addView(hourlyTile); addView(verdictTile)
    }

    val tagsText = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(4))
    }
    val routeText = TextView(context).apply {
        textSize = 11.5f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(6))
    }
    val verdictText = TextView(context).apply {
        textSize = 12f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    val extraText = TextView(context).apply {
        textSize = 11f; fontFeatureSettings = "tnum"; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(3), 0, 0)
    }
    val infoBox = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(9), dp(12), dp(9))
        background = GradientDrawable().apply { cornerRadius = dp(16).toFloat() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
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
        root, cardBg, topRow, statusHalo, statusDot, leftBadge, cutoutSpacer, rightBadge, appLogo, closeBtn,
        heroRow, amountText, heroRouteText,
        metricsRow, perMileTile, hourlyTile, verdictTile, tagsText, routeText, infoBox, verdictText, extraText,
        actionsRow, pauseBtn, declineBtn, switchBtn,
    )
}
