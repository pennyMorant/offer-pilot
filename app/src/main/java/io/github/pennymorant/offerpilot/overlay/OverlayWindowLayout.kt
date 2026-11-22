package io.github.pennymorant.offerpilot.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import kotlin.math.min
import kotlin.math.roundToInt

/** Positions the ordinary overlay inside the display contour, without covering system windows. */
internal class OverlayWindowLayout(
    private val context: Context,
    private val manager: WindowManager,
    private val views: OverlayViews,
) : ComponentCallbacks {
    private var expanded = false
    private var compactWidthDp = 168
    private var attached = false
    private var topLeft = 0f
    private var topRight = 0f
    private val params = WindowManager.LayoutParams(
        dp(compactWidthDp), WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        // Place the background behind the status region; reserve its full height inside the view.
        setFitInsetsTypes(0)
    }
    private val sizeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyCorners() }

    fun attach() {
        refresh()
        manager.addView(views.root, params)
        attached = true
        views.root.addOnLayoutChangeListener(sizeListener)
        views.root.setOnApplyWindowInsetsListener { _, insets -> refresh(); insets }
        context.registerComponentCallbacks(this)
        views.root.requestApplyInsets()
    }

    fun update(expanded: Boolean, compactWidthDp: Int) {
        if (this.expanded == expanded && this.compactWidthDp == compactWidthDp) return
        if (attached && views.root.isLaidOut) {
            TransitionManager.beginDelayedTransition(views.root, AutoTransition().apply { duration = 180 })
        }
        this.expanded = expanded
        this.compactWidthDp = compactWidthDp
        refresh()
    }

    private fun refresh() {
        val metrics = manager.maximumWindowMetrics
        val insets = metrics.windowInsets
        val cutout = insets.displayCutout
        val edge = dp(8)
        val side = edge + maxOf(cutout?.safeInsetLeft ?: 0, cutout?.safeInsetRight ?: 0)
        val availableWidth = (metrics.bounds.width() - side * 2).coerceAtLeast(1)
        val targetWidth = if (expanded) availableWidth else dp(compactWidthDp).coerceAtMost(availableWidth)
        val statusBottom = maxOf(insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top,
            cutout?.safeInsetTop ?: 0, dp(24))
        val topPadding = statusBottom - edge + dp(8)
        val changed = params.width != targetWidth || params.y != edge
        params.width = targetWidth
        params.y = edge
        // Equal top/side gaps yield concentric arcs: inner radius = display radius - gap.
        val cornerGap = maxOf(edge, side)
        fun radius(position: Int) = ((insets.getRoundedCorner(position)?.radius ?: dp(32)) - cornerGap)
            .coerceAtLeast(0).toFloat()
        topLeft = radius(RoundedCorner.POSITION_TOP_LEFT)
        topRight = radius(RoundedCorner.POSITION_TOP_RIGHT)
        if (views.root.paddingTop != topPadding) views.root.setPadding(dp(16), topPadding, dp(16), dp(16))
        applyCorners()
        if (attached && changed) runCatching { manager.updateViewLayout(views.root, params) }
    }

    private fun applyCorners() {
        val height = views.root.height.takeIf { it > 0 } ?: return
        val limit = min(params.width, height) / 2f
        val left = if (expanded) min(topLeft, limit) else limit
        val right = if (expanded) min(topRight, limit) else limit
        views.cardBg.cornerRadii = floatArrayOf(left, left, right, right, right, right, left, left)
        views.root.invalidateOutline()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        views.root.post { if (attached) refresh() }
    }

    override fun onLowMemory() = Unit

    fun close() {
        context.unregisterComponentCallbacks(this)
        views.root.setOnApplyWindowInsetsListener(null)
        views.root.removeOnLayoutChangeListener(sizeListener)
        if (attached) runCatching { manager.removeView(views.root) }
        attached = false
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
}
