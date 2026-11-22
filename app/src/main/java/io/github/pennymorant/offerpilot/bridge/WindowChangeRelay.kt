package io.github.pennymorant.offerpilot.bridge

import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import io.github.pennymorant.offerpilot.model.Platform
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Wake-up only: no order text or node handles cross the callback. */
class WindowChangeRelay(looper: Looper) {
    private val handler = Handler(looper)
    private val listener = AtomicReference<IWindowEvents?>()
    private val scheduled = AtomicBoolean(false)
    private var sequence = 0L

    fun register(value: IWindowEvents?) { listener.set(value) }

    fun onEvent(event: AccessibilityEvent) {
        val type = event.eventType
        val windowChange = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        if (!windowChange && Platform.fromPackage(event.packageName?.toString().orEmpty()) == null) return
        if (type and EVENT_TYPES == 0 || listener.get() == null || !scheduled.compareAndSet(false, true)) return
        handler.postDelayed({
            scheduled.set(false)
            val target = listener.get() ?: return@postDelayed
            runCatching { target.onWindowChanged(++sequence) }.onFailure { listener.compareAndSet(target, null) }
        }, 120)
    }

    fun close() { listener.set(null); handler.removeCallbacksAndMessages(null); scheduled.set(false) }

    companion object {
        const val EVENT_TYPES = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
            AccessibilityEvent.TYPE_VIEW_SCROLLED or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
    }
}
