package com.overud.capture

import com.overud.model.*
import com.overud.work.WorkSessionController
import java.util.concurrent.atomic.AtomicBoolean

/** A watchdog can invalidate work without acquiring the mutex held by blocked IPC. */
class CaptureLease(private val session: WorkSessionController) {
    private val expired = AtomicBoolean(false)
    private val generation = session.workGeneration
    fun current() = !expired.get() && session.workGeneration == generation && session.state.value.working && session.canInteract()
    fun valid(): Boolean {
        if (expired.get() || session.workGeneration != generation || !session.state.value.working) return false
        if (!session.canInteract()) {
            expire("设备已锁定或熄屏，自动接单已关闭；解锁后需手动开启")
            return false
        }
        return true
    }
    fun expire(message: String) {
        expired.set(true)
        session.update { state ->
            if (!state.working || session.workGeneration != generation) state else state.copy(automatic = false, autoAcceptEnabled = false,
                latest = null, decision = null, currentPage = null, destinationEvidence = null,
                destinationAssessment = DestinationAssessment(),
                deliveryPhase = if (state.pending) DeliveryPhase.UNCERTAIN else state.deliveryPhase,
                message = message)
        }
    }
}
