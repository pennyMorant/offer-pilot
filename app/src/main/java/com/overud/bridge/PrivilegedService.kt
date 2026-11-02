package com.overud.bridge

import android.app.UiAutomation
import android.graphics.Bitmap
import android.os.Bundle
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import com.overud.domain.Platform
import com.overud.domain.fingerprint

/** Runs under the Shevery/Shizuku shell identity. Hidden APIs stay in this boundary. */
class PrivilegedService : IPrivilegedService.Stub() {
    private val thread = HandlerThread("overud-automation").apply { start() }
    private var automation: UiAutomation? = null
    private val supported = Platform.entries.map { it.packageName }.toSet()

    @Synchronized private fun connection(): UiAutomation {
        automation?.let { return it }
        val connectionType = Class.forName("android.app.IUiAutomationConnection")
        val connection = Class.forName("android.app.UiAutomationConnection").getDeclaredConstructor().newInstance()
        val ui = UiAutomation::class.java.getDeclaredConstructor(android.os.Looper::class.java, connectionType)
            .newInstance(thread.looper, connection)
        // Do not suppress accessibility services that the user already has enabled.
        UiAutomation::class.java.getMethod("connect", Int::class.javaPrimitiveType).invoke(ui, 1)
        automation = ui
        return ui
    }

    private fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 35 || result.size >= 450) return
            result += node
            repeat(node.childCount.coerceAtMost(100)) { node.getChild(it)?.let { visit(it, depth + 1) } }
        }
        visit(root, 0)
        return result
    }

    private fun text(nodes: List<AccessibilityNodeInfo>) = nodes.flatMap {
        listOfNotNull(it.text?.toString(), it.contentDescription?.toString())
    }.filter(String::isNotBlank).distinct().joinToString("\n").take(30000)

    override fun snapshot(): Bundle = try {
        val root = connection().rootInActiveWindow
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

    override fun screenshot(expectedPackage: String): ParcelFileDescriptor? {
        if (expectedPackage !in supported) return null
        if (connection().rootInActiveWindow?.packageName?.toString() != expectedPackage) return null
        val bitmap = connection().takeScreenshot() ?: return null
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            try { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            finally { bitmap.recycle() }
        }.start()
        return pipe[0]
    }

    @Synchronized override fun accept(expectedToken: String, expectedPackage: String): String {
        val platform = Platform.fromPackage(expectedPackage) ?: return "unsupported"
        val root = connection().rootInActiveWindow ?: return "no_window"
        if (root.packageName?.toString() != expectedPackage) return "window_changed"
        val nodes = flatten(root)
        if (fingerprint(platform, text(nodes)) != expectedToken) return "offer_changed"
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

    override fun destroy() {
        runCatching { automation?.let { UiAutomation::class.java.getMethod("disconnect").invoke(it) } }
        thread.quitSafely()
        kotlin.system.exitProcess(0)
    }
}
