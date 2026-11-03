package com.overud.bridge

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.overud.model.Platform
import com.overud.platform.fingerprint
import com.overud.platform.NodePageAssembler

class WindowReader(private val connection: UiAutomationConnection) {
    private val supported = Platform.entries.map { it.packageName }.toSet()
    fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            check(depth <= 35 && result.size < 450 && node.childCount <= 100) { "window_node_limit" }
            result += node
            repeat(node.childCount.coerceAtMost(100)) { node.getChild(it)?.let { visit(it, depth + 1) } }
        }
        visit(root, 0)
        return result
    }

    fun text(nodes: List<AccessibilityNodeInfo>): String {
        val platform = Platform.fromPackage(nodes.firstOrNull()?.packageName?.toString().orEmpty()) ?: return ""
        return NodePageAssembler.assemble(platform, WindowNodeMapper.map(nodes)).text
    }

    fun snapshot(includeNodes: Boolean = false): Bundle = try {
        val root = connection.get().rootInActiveWindow
        val pkg = root?.packageName?.toString().orEmpty()
        if (root == null || pkg !in supported) Bundle().apply { putString("package", pkg) }
        else {
            val nodes = WindowNodeMapper.map(flatten(root))
            val platform = Platform.fromPackage(pkg)!!
            val page = NodePageAssembler.assemble(platform, nodes)
            val body = page.text
            Bundle().apply {
                putString("package", pkg); putString("text", body)
                putString("token", fingerprint(Platform.fromPackage(pkg)!!, body))
                putLong("capturedAt", System.currentTimeMillis())
                putInt("nodeCount", nodes.size)
                putInt("selectedRoot", page.selectedRoot ?: -1)
                putInt("windowId", root.windowId)
                putInt("displayId", root.window?.displayId ?: -1)
                putString("frameToken", fingerprint(platform, nodes.joinToString("\n")))
                val crop = page.ocrRoot?.let { index -> nodes.firstOrNull { it.index == index }?.bounds }
                if (crop != null) putIntArray("ocrBounds", intArrayOf(crop.left, crop.top, crop.right, crop.bottom))
                if (includeNodes) putParcelableArrayList("nodes", WindowDiagnosticCodec.encode(nodes))
            }
        }
    } catch (e: Exception) { Bundle().apply { putString("error", e.cause?.message ?: e.message ?: e.javaClass.simpleName) } }

}
