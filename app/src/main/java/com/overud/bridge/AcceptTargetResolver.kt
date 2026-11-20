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

        val dispatched = if (clickable != null && clickable.refresh() &&
            eligible(clickable, expectedPackage, latest.windowId) && clickable.isClickable) {
            clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            injectTap(label)
        }
        if (dispatched && initial != null) pending = Pending(expectedToken, initial, startedAt)
        return if (dispatched) "dispatched" else "click_failed"
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

    private fun matches(platform: Platform, node: AccessibilityNodeInfo, confirmation: Boolean): Boolean {
        val labels = listOf(node.text?.toString(), node.contentDescription?.toString())
        return labels.any {
            if (confirmation) AcceptanceConfirmationPolicy.matchesButton(platform, it)
            else AcceptanceActionPolicy.matchesInitialButton(platform, it)
        }
    }

    private fun eligible(node: AccessibilityNodeInfo, expectedPackage: String, expectedWindow: Int) =
        node.isVisibleToUser && node.isEnabled && node.packageName?.toString() == expectedPackage &&
            node.windowId == expectedWindow
}
