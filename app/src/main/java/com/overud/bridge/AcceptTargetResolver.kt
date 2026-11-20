package com.overud.bridge

import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.overud.model.Offer
import com.overud.model.Platform
import com.overud.platform.AcceptanceActionPolicy
import com.overud.platform.AcceptanceConfirmationPolicy
import com.overud.platform.OfferParser
import com.overud.platform.fingerprint

class AcceptTargetResolver(
    private val connection: UiAutomationConnection,
    private val reader: WindowReader,
) {
    private data class Pending(val token: String, val offer: Offer, val startedAt: Long)
    private var pending: Pending? = null

    @Synchronized fun accept(expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String {
        pending = null
        return click(expectedToken, expectedPackage, deadlineElapsed)
    }

    @Synchronized fun confirm(originalToken: String, expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String {
        val context = pending ?: return "confirmation_context_missing"
        pending = null // Consume before any action; an uncertain result must never be retried.
        if (context.token != originalToken || context.offer.platform.packageName != expectedPackage ||
            SystemClock.elapsedRealtime() - context.startedAt !in 0 until AcceptanceConfirmationPolicy.WINDOW_MILLIS) {
            return "confirmation_context_expired"
        }
        return click(expectedToken, expectedPackage, deadlineElapsed, context)
    }

    /** Manual one-tap decline: verifies live offer fingerprint before clicking Decline / Close. */
    @Synchronized fun decline(expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String {
        if (deadlineElapsed - SystemClock.elapsedRealtime() !in 1..2000) return "action_expired"
        val platform = Platform.fromPackage(expectedPackage) ?: return "unsupported"
        val root = reader.findTargetRoot(expectedPackage) ?: return "no_window"
        val nodes = reader.flatten(root)
        val body = reader.text(nodes)
        if (fingerprint(platform, body) != expectedToken) return "offer_changed"
        if (!AcceptanceActionPolicy.allowsInitialClick(OfferParser.classify(platform, body))) return "page_changed"
        val targets = nodes.filter { eligible(it, expectedPackage, root.windowId) && matchesDecline(it) }
        val label = targets.singleOrNull() ?: return "ambiguous_decline"
        val clickable = findClickableAncestor(label, expectedPackage, root.windowId)
        val latest = reader.findTargetRoot(expectedPackage) ?: return "no_window"
        if (latest.windowId != root.windowId || fingerprint(platform, reader.text(reader.flatten(latest))) != expectedToken) {
            return "offer_changed"
        }
        if (!label.refresh() || !eligible(label, expectedPackage, latest.windowId) || !matchesDecline(label)) {
            return "button_changed"
        }
        if (SystemClock.elapsedRealtime() >= deadlineElapsed) return "action_expired"
        val ok = if (clickable != null && clickable.refresh() &&
            eligible(clickable, expectedPackage, latest.windowId) && clickable.isClickable) {
            clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else injectTap(label)
        return if (ok) "declined" else "click_failed"
    }

    private fun click(expectedToken: String, expectedPackage: String, deadlineElapsed: Long, confirmation: Pending? = null): String {
        if (deadlineElapsed - SystemClock.elapsedRealtime() !in 1..2000) return "action_expired"
        val platform = Platform.fromPackage(expectedPackage) ?: return "unsupported"
        val root = reader.findTargetRoot(expectedPackage) ?: return "no_window"
        if (root.packageName?.toString() != expectedPackage) return "window_changed"
        val nodes = reader.flatten(root)
        val body = reader.text(nodes)
        if (fingerprint(platform, body) != expectedToken) return "offer_changed"
        if (confirmation != null) {
            if (!AcceptanceConfirmationPolicy.matchesOffer(confirmation.offer, body)) return "confirmation_offer_changed"
        } else if (!AcceptanceActionPolicy.allowsInitialClick(OfferParser.classify(platform, body))) return "page_changed"
        val targets = nodes.filter {
            eligible(it, expectedPackage, root.windowId) && matches(platform, it, confirmation != null)
        }
        if (targets.size != 1) return "ambiguous_button"
        val label = targets.single()
        val clickable = findClickableAncestor(label, expectedPackage, root.windowId)
        val latest = reader.findTargetRoot(expectedPackage) ?: return "no_window"
        if (latest.packageName?.toString() != expectedPackage || latest.windowId != root.windowId) {
            return "window_changed"
        }
        if (fingerprint(platform, reader.text(reader.flatten(latest))) != expectedToken) return "offer_changed"
        if (!label.refresh() || !eligible(label, expectedPackage, latest.windowId) ||
            !matches(platform, label, confirmation != null)) {
            return "button_changed"
        }
        if (confirmation != null && SystemClock.elapsedRealtime() - confirmation.startedAt >=
            AcceptanceConfirmationPolicy.WINDOW_MILLIS) return "confirmation_context_expired"
        val initial = if (confirmation == null) OfferParser.parse(platform, body, System.currentTimeMillis()) else null
        val startedAt = SystemClock.elapsedRealtime()
        if (startedAt >= deadlineElapsed) return "action_expired"

        val isSlide = isSlideTarget(label)
        val dispatched = when {
            isSlide -> injectSwipe(label)
            clickable != null && clickable.refresh() &&
                eligible(clickable, expectedPackage, latest.windowId) && clickable.isClickable ->
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            else -> injectTap(label)
        }
        if (dispatched && initial != null) pending = Pending(expectedToken, initial, startedAt)
        return if (dispatched) "dispatched" else "click_failed"
    }

    private fun isSlideTarget(node: AccessibilityNodeInfo): Boolean {
        val combined = "${node.text.orEmpty()} ${node.contentDescription.orEmpty()}"
        return combined.contains("slide", ignoreCase = true) || combined.contains("滑动")
    }

    private fun findClickableAncestor(label: AccessibilityNodeInfo, pkg: String, windowId: Int): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = label
        repeat(4) {
            val node = current ?: return null
            if (!eligible(node, pkg, windowId)) return null
            if (node.isClickable) return node
            current = node.parent
        }
        return null
    }

    /** Real touch fallback via UiAutomation (UID 2000) for custom-drawn or Compose buttons. */
    private fun injectTap(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also(node::getBoundsInScreen)
        if (bounds.isEmpty || bounds.width() < 4 || bounds.height() < 4) return false
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        val now = SystemClock.uptimeMillis()
        val ui = connection.get()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        val up = MotionEvent.obtain(now, now + 45, MotionEvent.ACTION_UP, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        return try {
            ui.injectInputEvent(down, true) && ui.injectInputEvent(up, true)
        } finally {
            down.recycle()
            up.recycle()
        }
    }

    /** Horizontal slide gesture via UiAutomation (UID 2000) for Slide-to-Accept controls. */
    private fun injectSwipe(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also(node::getBoundsInScreen)
        if (bounds.isEmpty || bounds.width() < 40 || bounds.height() < 8) return false
        val startX = bounds.left + bounds.width() * 0.12f
        val endX = bounds.right - bounds.width() * 0.12f
        val y = bounds.exactCenterY()
        val downTime = SystemClock.uptimeMillis()
        val ui = connection.get()
        fun event(time: Long, action: Int, x: Float) =
            MotionEvent.obtain(downTime, time, action, x, y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        val events = mutableListOf(event(downTime, MotionEvent.ACTION_DOWN, startX))
        val steps = 6
        for (i in 1..steps) {
            val fraction = i.toFloat() / steps
            events += event(downTime + i * 28L, MotionEvent.ACTION_MOVE, startX + (endX - startX) * fraction)
        }
        events += event(downTime + (steps + 1) * 28L, MotionEvent.ACTION_UP, endX)
        return try {
            events.all { ui.injectInputEvent(it, true) }
        } finally {
            events.forEach(MotionEvent::recycle)
        }
    }

    private fun matches(platform: Platform, node: AccessibilityNodeInfo, confirmation: Boolean): Boolean {
        val labels = listOf(node.text?.toString(), node.contentDescription?.toString())
        return labels.any {
            if (confirmation) AcceptanceConfirmationPolicy.matchesButton(platform, it)
            else AcceptanceActionPolicy.matchesInitialButton(platform, it)
        }
    }

    private fun matchesDecline(node: AccessibilityNodeInfo): Boolean {
        val labels = listOf(node.text?.toString(), node.contentDescription?.toString())
        val targets = setOf("decline", "decline offer", "close", "dismiss", "拒绝", "跳过", "×", "✕", "x")
        return labels.any { it?.trim()?.lowercase(java.util.Locale.ROOT) in targets }
    }

    private fun eligible(node: AccessibilityNodeInfo, expectedPackage: String, expectedWindow: Int) =
        node.isVisibleToUser && node.isEnabled && node.packageName?.toString() == expectedPackage &&
            node.windowId == expectedWindow
}
