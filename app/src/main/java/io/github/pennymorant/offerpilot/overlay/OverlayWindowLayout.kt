package io.github.pennymorant.offerpilot.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.RectF
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
    private var compactCameraCenterY: Float? = null
    private var lastHeight = 0
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
        val pathBounds = cutout?.cutoutPath?.let { path ->
            RectF().also { path.computeBounds(it, true) }.takeIf { it.width() > 0f && it.height() > 0f }
        }
        val centerCutout = cutout?.boundingRects?.firstOrNull { rect ->
            rect.width() > 0 && abs(rect.centerX() - screenWidth / 2) <= dp(48)
        }
        val cutoutWidth = when {
            pathBounds != null && abs(pathBounds.centerX() - screenWidth / 2f) <= dp(48) -> pathBounds.width().roundToInt()
            centerCutout != null -> centerCutout.width()
            else -> 0
        }
        compactCameraCenterY = when {
            pathBounds != null && abs(pathBounds.centerX() - screenWidth / 2f) <= dp(48) -> pathBounds.centerY()
            centerCutout != null -> centerCutout.centerY().toFloat()
            else -> null
        }
        val edge = dp(8)
        val side = edge + maxOf(cutout?.safeInsetLeft ?: 0, cutout?.safeInsetRight ?: 0)
        val availableWidth = (screenWidth - side * 2).coerceAtLeast(1)
        val baseCompact = dp(compactWidthDp)
        val targetWidth = if (expanded) availableWidth else {
            val withCutout = if (cutoutWidth > 0) maxOf(baseCompact, cutoutWidth + dp(84)) else baseCompact
            withCutout.coerceAtMost(availableWidth)
        }
        val statusBottom = maxOf(insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top,
            cutout?.safeInsetTop ?: 0, dp(24))
        val targetY = computeTargetY(edge, views.root.height)
        val changed = params.width != targetWidth || params.y != targetY
        params.width = targetWidth
        params.y = targetY
        val cornerGap = maxOf(edge, side)
        fun radius(position: Int) = ((insets.getRoundedCorner(position)?.radius ?: dp(32)) - cornerGap)
            .coerceAtLeast(0).toFloat()
        topLeft = radius(RoundedCorner.POSITION_TOP_LEFT)
        topRight = radius(RoundedCorner.POSITION_TOP_RIGHT)
        if (!expanded && cutoutWidth > 0) {
            val spacerWidth = cutoutWidth + dp(14)
            if (views.cutoutSpacer.visibility != View.VISIBLE || views.cutoutSpacer.layoutParams.width != spacerWidth) {
                views.cutoutSpacer.visibility = View.VISIBLE
                views.cutoutSpacer.layoutParams = views.cutoutSpacer.layoutParams.apply { width = spacerWidth }
            }
            val minPad = pathBounds?.let { ((it.height() + dp(10) - dp(24)) / 2f).roundToInt().coerceAtLeast(dp(6)) } ?: dp(6)
            if (views.root.paddingTop != minPad || views.root.paddingLeft != dp(12)) {
                views.root.setPadding(dp(12), minPad, dp(12), minPad)
            }
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

    private fun computeTargetY(edge: Int, height: Int): Int {
        if (expanded) return edge
        val centerY = compactCameraCenterY ?: return dp(6)
        val h = if (height > 0) height else dp(38)
        return (centerY - h / 2f).roundToInt().coerceAtLeast(dp(2))
    }

    private fun applyCorners() {
        val height = views.root.height.takeIf { it > 0 } ?: return
        val yChanged = if (!expanded && height != lastHeight) {
            lastHeight = height
            val targetY = computeTargetY(dp(8), height)
            if (params.y != targetY) {
                params.y = targetY
                true
            } else false
        } else false
        val limit = min(params.width, height) / 2f
        val left = if (expanded) min(topLeft, limit) else limit
        val right = if (expanded) min(topRight, limit) else limit
        views.cardBg.cornerRadii = floatArrayOf(left, left, right, right, right, right, left, left)
        views.root.invalidateOutline()
        if (attached && yChanged) runCatching { manager.updateViewLayout(views.root, params) }
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
