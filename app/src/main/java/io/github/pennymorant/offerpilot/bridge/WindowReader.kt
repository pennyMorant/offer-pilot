package io.github.pennymorant.offerpilot.bridge

import android.os.Bundle
import android.os.DeadObjectException
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.platform.NodePageAssembler
import io.github.pennymorant.offerpilot.platform.fingerprint

class WindowReader(private val connection: UiAutomationConnection) {
    private val supported = Platform.entries.map { it.packageName }.toSet()

    /** Multi-window scan: prefers top visible Uber/DoorDash window even when overlays are present. */
    fun findTargetRoot(expectedPackage: String? = null): AccessibilityNodeInfo? {
        val ui = connection.get()
        val windows = runCatching { ui.windows }.getOrNull().orEmpty()
        val target = windows
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION || it.type == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER }
            .sortedByDescending { it.layer }
            .firstNotNullOfOrNull { win ->
                val root = runCatching { win.root }.getOrNull() ?: return@firstNotNullOfOrNull null
                val pkg = root.packageName?.toString().orEmpty()
                val matches = if (expectedPackage != null) pkg == expectedPackage else pkg in supported
                if (matches) root else null
            }
        if (target != null) return target
        val active = ui.rootInActiveWindow ?: return null
        val pkg = active.packageName?.toString().orEmpty()
        val matches = if (expectedPackage != null) pkg == expectedPackage else pkg in supported
        return if (matches) active else null
    }

    /** Safe bounded traversal: skips invisible subtrees and caps gracefully instead of crashing. */
    fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_DEPTH || result.size >= MAX_NODES) return
            if (depth > 0 && !node.isVisibleToUser) return
            result += node
            val count = node.childCount.coerceAtMost(MAX_CHILDREN)
            for (i in 0 until count) {
                if (result.size >= MAX_NODES) return
                node.getChild(i)?.let { visit(it, depth + 1) }
            }
        }
        visit(root, 0)
        return result
    }

    fun text(nodes: List<AccessibilityNodeInfo>): String {
        val platform = Platform.fromPackage(nodes.firstOrNull()?.packageName?.toString().orEmpty()) ?: return ""
        return NodePageAssembler.assemble(platform, WindowNodeMapper.map(nodes)).text
    }

    fun snapshot(includeNodes: Boolean = false): Bundle = try {
        val root = findTargetRoot()
        val pkg = root?.packageName?.toString().orEmpty()
        if (root == null || pkg !in supported) {
            val activePkg = runCatching { connection.get().rootInActiveWindow?.packageName?.toString().orEmpty() }.getOrDefault("")
            Bundle().apply { putString("package", activePkg) }
        } else {
            val nodes = WindowNodeMapper.map(flatten(root))
            val platform = Platform.fromPackage(pkg)!!
            val page = NodePageAssembler.assemble(platform, nodes)
            val body = page.text
            Bundle().apply {
                putString("package", pkg); putString("text", body)
                putString("token", fingerprint(platform, body))
                putLong("capturedAt", System.currentTimeMillis())
                putInt("nodeCount", nodes.size)
                putInt("selectedRoot", page.selectedRoot ?: -1)
                putInt("windowId", root.windowId)
                putInt("displayId", root.window?.displayId ?: 0)
                // Stable window identity (does not churn every second on offer countdown timers).
                putString("frameToken", fingerprint(platform, "$pkg|${root.windowId}|${root.window?.displayId ?: 0}"))
                val crop = page.ocrRoot?.let { index -> nodes.firstOrNull { it.index == index }?.bounds }
                if (crop != null) putIntArray("ocrBounds", intArrayOf(crop.left, crop.top, crop.right, crop.bottom))
                if (includeNodes) putParcelableArrayList("nodes", WindowDiagnosticCodec.encode(nodes))
            }
        }
    } catch (e: Exception) {
        if (e is IllegalStateException || e is DeadObjectException || e.cause is DeadObjectException) {
            connection.reset()
        }
        Bundle().apply { putString("error", e.cause?.message ?: e.message ?: e.javaClass.simpleName) }
    }

    companion object {
        private const val MAX_DEPTH = 45
        private const val MAX_NODES = 800
        private const val MAX_CHILDREN = 120
    }
}
