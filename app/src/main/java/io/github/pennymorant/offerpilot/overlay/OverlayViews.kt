package io.github.pennymorant.offerpilot.overlay

import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView

internal class OverlayViews(
    val root: LinearLayout,
    val cardBg: GradientDrawable,
    val verdictBadge: TextView,
    val scoreBadge: TextView,
    val closeBtn: TextView,
    val reasonText: TextView,
    val amountBox: LinearLayout,
    val amountLabel: TextView,
    val amountValue: TextView,
    val perMileBox: LinearLayout,
    val perMileLabel: TextView,
    val perMileValue: TextView,
    val hourlyBox: LinearLayout,
    val hourlyLabel: TextView,
    val hourlyValue: TextView,
    val routeLine: TextView,
)
