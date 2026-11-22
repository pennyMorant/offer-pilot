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
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
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
            TransitionManager.beginDelayedTransition(
                views.root,
                AutoTransition().apply { duration = 220; interpolator = OvershootInterpolator(0.55f) },
            )
        }
        this.expanded = expanded
        this.compactWidthDp = compactWidthDp
        refresh()
    }

    private fun refresh() {
        val metrics = manager.maximumWindowMetrics
        val insets = metrics.windowInsets
        val cutout = insets.displayCutout
        val screenWidth = metrics.bounds.width()
        val centerCutout = cutout?.boundingRects?.firstOrNull { rect ->
            rect.width() > 0 && abs(rect.centerX() - screenWidth / 2) <= dp(48)
        }
        val edge = dp(8)
        val side = edge + maxOf(cutout?.safeInsetLeft ?: 0, cutout?.safeInsetRight ?: 0)
        val availableWidth = (screenWidth - side * 2).coerceAtLeast(1)
        val baseCompact = dp(compactWidthDp)
        val targetWidth = if (expanded) availableWidth else {
            val withCutout = if (centerCutout != null) maxOf(baseCompact, centerCutout.width() + dp(164)) else baseCompact
            withCutout.coerceAtMost(availableWidth)
        }
        val statusBottom = maxOf(insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top,
            cutout?.safeInsetTop ?: 0, dp(24))
        val targetY = if (!expanded && centerCutout != null) (centerCutout.top - dp(4)).coerceAtLeast(dp(2)) else edge
        val changed = params.width != targetWidth || params.y != targetY
        params.width = targetWidth
        params.y = targetY
        val cornerGap = maxOf(edge, side)
        fun radius(position: Int) = ((insets.getRoundedCorner(position)?.radius ?: dp(32)) - cornerGap)
            .coerceAtLeast(0).toFloat()
        topLeft = radius(RoundedCorner.POSITION_TOP_LEFT)
        topRight = radius(RoundedCorner.POSITION_TOP_RIGHT)
        if (!expanded && centerCutout != null) {
            val spacerWidth = centerCutout.width() + dp(12)
            if (views.cutoutSpacer.visibility != View.VISIBLE || views.cutoutSpacer.layoutParams.width != spacerWidth) {
                views.cutoutSpacer.visibility = View.VISIBLE
                views.cutoutSpacer.layoutParams = views.cutoutSpacer.layoutParams.apply { width = spacerWidth }
            }
            views.root.setPadding(dp(12), dp(5), dp(12), dp(5))
        } else {
            if (views.cutoutSpacer.visibility != View.GONE) views.cutoutSpacer.visibility = View.GONE
            val topPadding = if (expanded) (statusBottom - edge + dp(6)).coerceAtLeast(dp(12)) else dp(7)
            val horiz = if (expanded) dp(16) else dp(14)
            val bottom = if (expanded) dp(16) else dp(7)
            if (views.root.paddingTop != topPadding || views.root.paddingLeft != horiz) {
                views.root.setPadding(horiz, topPadding, horiz, bottom)
            }
        }
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
