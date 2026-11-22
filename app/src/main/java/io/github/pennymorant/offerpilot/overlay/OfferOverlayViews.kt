package io.github.pennymorant.offerpilot.overlay

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

internal fun buildOverlayViews(
    context: Context,
    onPause: () -> Unit,
    onDecline: () -> Unit,
    onSwipeUp: () -> Unit,
): OverlayViews {
    val d = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()
    val swipeThreshold = dp(32).toFloat()
    val cardBg = GradientDrawable()
    val root = object : LinearLayout(context) {
        private var startX = 0f
        private var startY = 0f
        private var triggered = false
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = ev.rawX; startY = ev.rawY; triggered = false }
                MotionEvent.ACTION_MOVE -> if (!triggered) {
                    val dx = ev.rawX - startX
                    val dy = ev.rawY - startY
                    if (dy < -swipeThreshold && abs(dy) > abs(dx)) {
                        triggered = true
                        onSwipeUp()
                        val cancel = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                        return true
                    }
                }
            }
            return if (triggered) true else super.dispatchTouchEvent(ev)
        }
    }.apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        clipToOutline = true
        background = cardBg
        elevation = dp(10).toFloat()
    }
    fun pill(size: Float) = TextView(context).apply {
        textSize = size; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(10), dp(4), dp(10), dp(4))
        background = GradientDrawable().apply { cornerRadius = dp(999).toFloat() }
    }
    fun metricBox(title: String, startMargin: Int): Triple<LinearLayout, TextView, TextView> {
        val label = TextView(context).apply { text = title; textSize = 11f; maxLines = 1 }
        val value = TextView(context).apply {
            textSize = 16.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(startMargin)
            }
            addView(label); addView(value)
        }
        return Triple(box, label, value)
    }
    fun actionPill(onClick: () -> Unit) = TextView(context).apply {
        textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; maxLines = 1
        minimumHeight = dp(44)
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply { cornerRadius = dp(999).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
        setOnClickListener { onClick() }
    }

    val leftBadge = pill(12f)
    val rightBadge = pill(11.5f).apply { gravity = Gravity.END }
    val closeBtn = TextView(context).apply {
        text = "✕"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
    }
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(leftBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        addView(rightBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(closeBtn, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(8) })
    }

    val amountText = TextView(context).apply {
        textSize = 32f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
    }
    val verdictTile = pill(12.5f).apply { setPadding(dp(12), dp(5), dp(12), dp(5)) }
    val heroRouteText = TextView(context).apply {
        textSize = 13f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
        setPadding(0, dp(4), 0, 0)
    }
    val heroRight = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.END
        addView(verdictTile, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(heroRouteText)
    }
    val heroRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(10), 0, dp(10))
        addView(amountText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(heroRight)
    }

    val (perMileBox, perMileLabel, perMileTile) = metricBox("每英里", 0)
    val (hourlyBox, hourlyLabel, hourlyTile) = metricBox("预计时薪", 8)
    val metricsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 0, 0, dp(10))
        addView(perMileBox); addView(hourlyBox)
    }

    val tagsText = TextView(context).apply {
        textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(4))
    }
    val routeText = TextView(context).apply {
        textSize = 12f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(8))
    }
    val verdictText = TextView(context).apply {
        textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    val extraText = TextView(context).apply {
        textSize = 11.5f; fontFeatureSettings = "tnum"; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(3), 0, 0)
    }
    val infoBox = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply { cornerRadius = dp(18).toFloat() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
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
        root, cardBg, topRow, leftBadge, rightBadge, closeBtn, heroRow, amountText, verdictTile, heroRouteText,
        metricsRow, perMileBox, perMileLabel, perMileTile, hourlyBox, hourlyLabel, hourlyTile,
        tagsText, routeText, infoBox, verdictText, extraText, actionsRow, pauseBtn, declineBtn, switchBtn,
    )
}
