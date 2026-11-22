package io.github.pennymorant.offerpilot.overlay

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

internal class OverlayViews(
    val root: LinearLayout,
    val cardBg: GradientDrawable,
    val topRow: LinearLayout,
    val leftBadge: TextView,
    val cutoutSpacer: View,
    val rightBadge: TextView,
    val heroRow: LinearLayout,
    val amountText: TextView,
    val heroRouteText: TextView,
    val metricsRow: LinearLayout,
    val perMileTile: TextView,
    val hourlyTile: TextView,
    val verdictTile: TextView,
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
