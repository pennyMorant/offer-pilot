package com.overud.bridge

import android.os.Bundle
import com.overud.model.NodeBounds
import com.overud.model.UiNodeSnapshot

/** Only requested by manual diagnostics, not included in ordinary snapshots. */
object WindowDiagnosticCodec {
    fun encode(nodes: List<UiNodeSnapshot>) = ArrayList(nodes.map { node -> Bundle().apply {
        putInt("index", node.index); putInt("parent", node.parentIndex)
        putString("text", node.text); putString("description", node.description)
        putString("viewId", node.viewId?.take(256)); putString("package", node.packageName?.take(256))
        putInt("windowId", node.windowId)
        putIntArray("bounds", intArrayOf(node.bounds.left, node.bounds.top, node.bounds.right, node.bounds.bottom))
        putBoolean("visible", node.visible); putBoolean("enabled", node.enabled); putBoolean("clickable", node.clickable)
    } })

    fun decode(snapshot: Bundle): List<UiNodeSnapshot> {
        val entries = snapshot.getParcelableArrayList("nodes", Bundle::class.java) ?: return emptyList()
        require(entries.size <= 450)
        var characters = 0
        return entries.mapIndexed { index, node ->
            require(node.getInt("index", -1) == index)
            val parent = node.getInt("parent", -1)
            require(parent in -1 until index)
            val bounds = node.getIntArray("bounds") ?: error("缺少节点边界")
            require(bounds.size == 4)
            val text = node.getString("text")
            val description = node.getString("description")
            characters += (text?.length ?: 0) + (description?.length ?: 0)
            require(characters <= 30_000)
            UiNodeSnapshot(index, parent, text, description, node.getString("viewId"), node.getString("package"),
                node.getInt("windowId"), NodeBounds(bounds[0], bounds[1], bounds[2], bounds[3]),
                node.getBoolean("visible"), node.getBoolean("enabled"), node.getBoolean("clickable"))
        }
    }
}
