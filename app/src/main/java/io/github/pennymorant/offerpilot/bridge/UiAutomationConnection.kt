package io.github.pennymorant.offerpilot.bridge

import android.app.UiAutomation
import android.os.HandlerThread
import android.accessibilityservice.AccessibilityServiceInfo

class UiAutomationConnection {
    private val thread = HandlerThread("overud-automation").apply { start() }
    val changes = WindowChangeRelay(thread.looper)
    private var automation: UiAutomation? = null
    @Synchronized fun get(): UiAutomation {
        automation?.let { return it }
        val connectionType = Class.forName("android.app.IUiAutomationConnection")
        val connection = Class.forName("android.app.UiAutomationConnection").getDeclaredConstructor().newInstance()
        val ui = UiAutomation::class.java.getDeclaredConstructor(android.os.Looper::class.java, connectionType)
            .newInstance(thread.looper, connection)
        // Do not suppress accessibility services that the user already has enabled.
        UiAutomation::class.java.getMethod("connect", Int::class.javaPrimitiveType).invoke(ui, 1)
        ui.serviceInfo = ui.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            eventTypes = WindowChangeRelay.EVENT_TYPES
            notificationTimeout = 100
        }
        ui.setOnAccessibilityEventListener(changes::onEvent)
        automation = ui
        return ui
    }

    /** Disconnect and drop the cached UiAutomation handle so the next call rebuilds cleanly. */
    @Synchronized fun reset() {
        val old = automation
        automation = null
        if (old != null) runCatching { UiAutomation::class.java.getMethod("disconnect").invoke(old) }
    }

    fun close() {
        changes.close()
        reset()
        thread.quitSafely()
    }
}
