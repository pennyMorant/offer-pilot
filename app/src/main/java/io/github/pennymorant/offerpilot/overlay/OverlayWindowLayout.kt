package io.github.pennymorant.offerpilot.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import kotlin.math.min
import kotlin.math.roundToInt

/** Concentric top-edge MD3 Expressive floating card covering the status bar safely above the cutout. */
internal class OverlayWindowLayout(
    private val context: Context,
    private val manager: WindowManager,
    private val views: OverlayViews,
) : ComponentCallbacks {
    private var attached = false
    private var topLeft = 0f
    private var topRight = 0f
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
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

    fun animateIn() {
        val v = views.root
        v.animate().cancel()
        v.translationY = -dp(20).toFloat()
        v.scaleX = 0.95f
        v.scaleY = 0.95f
        v.alpha = 0f
        v.animate()
            .translationY(0f).scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(240)
            .setInterpolator(OvershootInterpolator(0.75f))
            .start()
    }

    fun refresh() {
        val metrics = manager.maximumWindowMetrics
        val insets = metrics.windowInsets
        val cutout = insets.displayCutout
        val edge = dp(8)
        val side = edge + maxOf(cutout?.safeInsetLeft ?: 0, cutout?.safeInsetRight ?: 0)
        val targetWidth = (metrics.bounds.width() - side * 2).coerceAtLeast(1)
        val statusBottom = maxOf(
            insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top,
            cutout?.safeInsetTop ?: 0,
            dp(24),
        )
        val topPadding = (statusBottom - edge + dp(6)).coerceAtLeast(dp(14))
        val changed = params.width != targetWidth || params.y != edge
        params.width = targetWidth
        params.y = edge
        val cornerGap = maxOf(edge, side)
        fun radius(position: Int) = ((insets.getRoundedCorner(position)?.radius ?: dp(32)) - cornerGap)
            .coerceAtLeast(dp(20)).toFloat()
        topLeft = radius(RoundedCorner.POSITION_TOP_LEFT)
        topRight = radius(RoundedCorner.POSITION_TOP_RIGHT)
        if (views.root.paddingTop != topPadding) {
            views.root.setPadding(dp(18), topPadding, dp(18), dp(18))
        }
        applyCorners()
        if (attached && changed) runCatching { manager.updateViewLayout(views.root, params) }
    }

    private fun applyCorners() {
        val height = views.root.height.takeIf { it > 0 } ?: return
        val limit = min(params.width, height) / 2f
        val left = min(topLeft, limit)
        val right = min(topRight, limit)
        val bottom = min(dp(28).toFloat(), limit)
        views.cardBg.cornerRadii = floatArrayOf(left, left, right, right, bottom, bottom, bottom, bottom)
        views.root.invalidateOutline()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        views.root.post { if (attached) refresh() }
    }

    @Deprecated("Deprecated in Java")
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
