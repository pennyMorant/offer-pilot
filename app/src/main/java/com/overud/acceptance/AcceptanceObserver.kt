package com.overud.acceptance

import android.os.SystemClock
import com.overud.data.repository.AcceptanceRepository
import com.overud.model.*
import com.overud.platform.DeliveryScreenParser
import com.overud.platform.DeliveryScreenState
import com.overud.work.WorkSessionController

class AcceptanceObserver(
    private val repository: AcceptanceRepository,
    private val session: WorkSessionController,
    private val onConfirmed: () -> Unit,
    private val onCompleted: () -> Unit,
) {
    private val unresolved = setOf("dispatching", "outcome_unknown", "accepted")
    private var activeToken: String? = null
    private var completionSince: Long? = null
    private var lastCompletionAt = 0L
    private var lastActiveAt = 0L

    fun reset() { activeToken = null; completionSince = null; lastCompletionAt = 0; lastActiveAt = 0 }

    fun observe(platform: Platform, text: String) {
        if (!session.state.value.working) return
        val screen = DeliveryScreenParser.parse(text)
        var record = repository.active()
        if (screen == DeliveryScreenState.ACTIVE && (record == null || record.state !in unresolved)) {
            if (!repository.adoptActive(platform, session.state.value.policy.version)) return
            record = repository.active()
        }
        if (record == null || record.state !in unresolved || record.platform != platform.name) {
            completionSince = null
            return
        }
        when (screen) {
            DeliveryScreenState.ACTIVE, DeliveryScreenState.ACCEPTED -> {
                if (record.state != "accepted") repository.update(record.offerId, "accepted", "confirmed_delivery_screen")
                if (screen == DeliveryScreenState.ACTIVE) {
                    activeToken = record.offerId
                    lastActiveAt = SystemClock.elapsedRealtime()
                }
                completionSince = null
                onConfirmed()
                session.update { it.copy(pending = true, automatic = false, deliveryPhase = DeliveryPhase.ACTIVE,
                    message = "已识别进行中的配送，自动接单暂停") }
            }
            DeliveryScreenState.COMPLETED -> {
                if (record.state != "accepted" || activeToken != record.offerId) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastActiveAt !in 0..30_000) { completionSince = null; return }
                val first = completionSince
                if (first == null || now - lastCompletionAt > 3000) {
                    completionSince = now
                    lastCompletionAt = now
                    return
                }
                lastCompletionAt = now
                if (now - first < 600) return
                repository.update(record.offerId, "completed", "confirmed_completion_screen")
                reset()
                onCompleted()
                session.releaseDelivery("已识别配送完成")
            }
            DeliveryScreenState.UNKNOWN -> completionSince = null
        }
    }
}
