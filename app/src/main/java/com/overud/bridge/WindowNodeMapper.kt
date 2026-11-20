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
            val rawText = node.text?.toString()
            val rawDesc = node.contentDescription?.toString()
            val remaining = (30_000 - textSize).coerceAtLeast(0)
            val text = rawText?.take(remaining)
            textSize += text?.length ?: 0
            val desc = rawDesc?.take((30_000 - textSize).coerceAtLeast(0))
            textSize += desc?.length ?: 0
            val bounds = Rect().also(node::getBoundsInScreen)
            UiNodeSnapshot(index, node.parent?.let { indices[it] } ?: -1, text, desc,
                node.viewIdResourceName, node.packageName?.toString(), node.windowId,
                NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                node.isVisibleToUser, node.isEnabled, node.isClickable)
        }
    }
}
