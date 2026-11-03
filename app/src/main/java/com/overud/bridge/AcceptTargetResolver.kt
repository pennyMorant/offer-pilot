package com.overud.bridge

import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock
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
        val root = connection.get().rootInActiveWindow ?: return "no_window"
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
        var target: AccessibilityNodeInfo? = label
        repeat(4) {
            val current = target ?: return "missing_button"
            if (!eligible(current, expectedPackage, root.windowId)) return "button_unavailable"
            if (current.isClickable) {
                // Re-read the active window after resolving ancestors, before issuing the action.
                val latest = connection.get().rootInActiveWindow ?: return "no_window"
                if (latest.packageName?.toString() != expectedPackage || latest.windowId != root.windowId) {
                    return "window_changed"
                }
                if (fingerprint(platform, reader.text(reader.flatten(latest))) != expectedToken) return "offer_changed"
                if (!label.refresh() || !eligible(label, expectedPackage, latest.windowId) ||
                    !matches(platform, label, confirmation != null)) {
                    return "button_changed"
                }
                if (!current.refresh() || !eligible(current, expectedPackage, latest.windowId) || !current.isClickable) {
                    return "button_unavailable"
                }
                if (confirmation != null && SystemClock.elapsedRealtime() - confirmation.startedAt >=
                    AcceptanceConfirmationPolicy.WINDOW_MILLIS) return "confirmation_context_expired"
                val initial = if (confirmation == null) OfferParser.parse(platform, body, System.currentTimeMillis()) else null
                val startedAt = SystemClock.elapsedRealtime()
                if (startedAt >= deadlineElapsed) return "action_expired"
                val dispatched = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (dispatched && initial != null) pending = Pending(expectedToken, initial, startedAt)
                return if (dispatched) "dispatched" else "click_failed"
            }
            target = current.parent
        }
        return "button_not_clickable"
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
