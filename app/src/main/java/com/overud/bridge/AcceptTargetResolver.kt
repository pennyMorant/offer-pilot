package com.overud.bridge

import android.view.accessibility.AccessibilityNodeInfo
import com.overud.model.Platform
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
        if (fingerprint(platform, reader.text(nodes)) != expectedToken) return "offer_changed"
        val targets = nodes.filter {
            it.isVisibleToUser && it.isEnabled &&
                (it.text?.toString()?.trim()?.lowercase() in setOf("accept", "match", "接受", "接单") ||
                    it.contentDescription?.toString()?.trim()?.lowercase() in setOf("accept", "match", "接受", "接单"))
        }
        if (targets.size != 1) return "ambiguous_button"
        var target: AccessibilityNodeInfo? = targets.single()
        repeat(4) {
            val current = target ?: return "missing_button"
            if (current.isClickable) {
                return if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) "dispatched" else "click_failed"
            }
            target = current.parent
        }
        return "button_not_clickable"
    }

}
