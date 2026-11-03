package com.overud.bridge

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.overud.model.NodeBounds
import com.overud.model.UiNodeSnapshot

object WindowNodeMapper {
    fun map(nodes: List<AccessibilityNodeInfo>): List<UiNodeSnapshot> {
        val indices = nodes.withIndex().associate { it.value to it.index }
        var textSize = 0
        return nodes.mapIndexed { index, node ->
            val text = node.text?.toString()
            val description = node.contentDescription?.toString()
            textSize += (text?.length ?: 0) + (description?.length ?: 0)
            check(textSize <= 30_000) { "window_text_limit" }
            val bounds = Rect().also(node::getBoundsInScreen)
            UiNodeSnapshot(index, node.parent?.let { indices[it] } ?: -1, text, description,
                node.viewIdResourceName, node.packageName?.toString(), node.windowId,
                NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                node.isVisibleToUser, node.isEnabled, node.isClickable)
        }
    }
}
