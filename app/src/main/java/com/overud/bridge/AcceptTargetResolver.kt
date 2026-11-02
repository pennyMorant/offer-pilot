package com.overud.bridge

import android.view.accessibility.AccessibilityNodeInfo
import com.overud.model.Platform
import com.overud.platform.AcceptanceActionPolicy
import com.overud.platform.OfferParser
import com.overud.platform.fingerprint

class AcceptTargetResolver(
    private val connection: UiAutomationConnection,
    private val reader: WindowReader,
) {
    @Synchronized fun accept(expectedToken: String, expectedPackage: String): String {
        val platform = Platform.fromPackage(expectedPackage) ?: return "unsupported"
        val root = connection.get().rootInActiveWindow ?: return "no_window"
        if (root.packageName?.toString() != expectedPackage) return "window_changed"
        val nodes = reader.flatten(root)
        val body = reader.text(nodes)
        if (fingerprint(platform, body) != expectedToken) return "offer_changed"
        if (!AcceptanceActionPolicy.allowsInitialClick(OfferParser.classify(platform, body))) return "page_changed"
        val targets = nodes.filter {
            eligible(it, expectedPackage, root.windowId) && matches(platform, it)
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
                if (!label.refresh() || !eligible(label, expectedPackage, latest.windowId) || !matches(platform, label)) {
                    return "button_changed"
                }
                if (!current.refresh() || !eligible(current, expectedPackage, latest.windowId) || !current.isClickable) {
                    return "button_unavailable"
                }
                return if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) "dispatched" else "click_failed"
            }
            target = current.parent
        }
        return "button_not_clickable"
    }

    private fun matches(platform: Platform, node: AccessibilityNodeInfo) =
        AcceptanceActionPolicy.matchesInitialButton(platform, node.text?.toString()) ||
            AcceptanceActionPolicy.matchesInitialButton(platform, node.contentDescription?.toString())

    private fun eligible(node: AccessibilityNodeInfo, expectedPackage: String, expectedWindow: Int) =
        node.isVisibleToUser && node.isEnabled && node.packageName?.toString() == expectedPackage &&
            node.windowId == expectedWindow
}
