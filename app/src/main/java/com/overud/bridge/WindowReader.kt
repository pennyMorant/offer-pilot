package com.overud.bridge

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.overud.model.Platform
import com.overud.platform.fingerprint

class WindowReader(private val connection: UiAutomationConnection) {
    private val supported = Platform.entries.map { it.packageName }.toSet()
    fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 35 || result.size >= 450) return
            result += node
            repeat(node.childCount.coerceAtMost(100)) { node.getChild(it)?.let { visit(it, depth + 1) } }
        }
        visit(root, 0)
        return result
    }

    fun text(nodes: List<AccessibilityNodeInfo>) = nodes.flatMap {
        listOfNotNull(it.text?.toString(), it.contentDescription?.toString())
    }.filter(String::isNotBlank).distinct().joinToString("\n").take(30000)

    fun snapshot(): Bundle = try {
        val root = connection.get().rootInActiveWindow
        val pkg = root?.packageName?.toString().orEmpty()
        if (root == null || pkg !in supported) Bundle().apply { putString("package", pkg) }
        else {
            val body = text(flatten(root))
            Bundle().apply {
                putString("package", pkg); putString("text", body)
                putString("token", fingerprint(Platform.fromPackage(pkg)!!, body))
                putLong("capturedAt", System.currentTimeMillis())
            }
        }
    } catch (e: Exception) { Bundle().apply { putString("error", e.cause?.message ?: e.message ?: e.javaClass.simpleName) } }

}
