package io.github.pennymorant.offerpilot.overlay

import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView

internal class OverlayViews(
    val root: LinearLayout,
    val cardBg: GradientDrawable,
    val topRow: LinearLayout,
    val leftBadge: TextView,
    val rightBadge: TextView,
    val closeBtn: TextView,
    val heroRow: LinearLayout,
    val amountText: TextView,
    val verdictTile: TextView,
    val heroRouteText: TextView,
    val metricsRow: LinearLayout,
    val perMileBox: LinearLayout,
    val perMileLabel: TextView,
    val perMileTile: TextView,
    val hourlyBox: LinearLayout,
    val hourlyLabel: TextView,
    val hourlyTile: TextView,
    val tagsText: TextView,
    val routeText: TextView,
    val infoBox: LinearLayout,
    val verdictText: TextView,
    val extraText: TextView,
    val actionsRow: LinearLayout,
    val pauseBtn: TextView,
    val declineBtn: TextView,
    val switchBtn: TextView,
)
