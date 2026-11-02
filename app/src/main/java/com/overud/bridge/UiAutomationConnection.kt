package com.overud.bridge

import android.app.UiAutomation
import android.os.HandlerThread

class UiAutomationConnection {
    private val thread = HandlerThread("overud-automation").apply { start() }
    private var automation: UiAutomation? = null
    @Synchronized fun get(): UiAutomation {
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

    fun close() {
        runCatching { automation?.let { UiAutomation::class.java.getMethod("disconnect").invoke(it) } }
        thread.quitSafely()
    }
}
