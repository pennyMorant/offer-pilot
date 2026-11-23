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
        setPadding(dp(18), dp(18), dp(18), dp(16))
        clipToOutline = true
        background = cardBg
        elevation = dp(10).toFloat()
    }

    val verdictBadge = TextView(context).apply {
        textSize = 18f; typeface = Typeface.DEFAULT_BOLD; maxLines = 1
        setPadding(dp(16), dp(7), dp(16), dp(7))
        background = GradientDrawable().apply { cornerRadius = dp(999).toFloat() }
    }
    val scoreBadge = TextView(context).apply {
        textSize = 14f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
        setPadding(dp(12), dp(6), dp(12), dp(6))
        background = GradientDrawable().apply { cornerRadius = dp(999).toFloat() }
    }
    val closeBtn = TextView(context).apply {
        text = "✕"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
    }
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(verdictBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(scoreBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(8)
        })
        addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        addView(closeBtn, LinearLayout.LayoutParams(dp(32), dp(32)))
    }

    val reasonText = TextView(context).apply {
        textSize = 14f; typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(2), dp(8), dp(2), dp(12))
    }

    fun metricBox(title: String, valueSp: Float, startMargin: Int): Triple<LinearLayout, TextView, TextView> {
        val label = TextView(context).apply { text = title; textSize = 11f; maxLines = 1 }
        val value = TextView(context).apply {
            textSize = valueSp; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"; maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(startMargin)
            }
            addView(label); addView(value)
        }
        return Triple(box, label, value)
    }

    val (amountBox, amountLabel, amountValue) = metricBox("金额", 20f, 0)
    val (perMileBox, perMileLabel, perMileValue) = metricBox("单价", 18f, 8)
    val (hourlyBox, hourlyLabel, hourlyValue) = metricBox("时薪", 18f, 8)
    val metricsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(amountBox); addView(perMileBox); addView(hourlyBox)
    }

    val routeLine = TextView(context).apply {
        textSize = 12f; fontFeatureSettings = "tnum"; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(2), dp(10), dp(2), 0)
    }

    root.addView(headerRow); root.addView(reasonText); root.addView(metricsRow); root.addView(routeLine)
    return OverlayViews(
        root, cardBg, verdictBadge, scoreBadge, closeBtn, reasonText,
        amountBox, amountLabel, amountValue,
        perMileBox, perMileLabel, perMileValue,
        hourlyBox, hourlyLabel, hourlyValue,
        routeLine,
    )
}
