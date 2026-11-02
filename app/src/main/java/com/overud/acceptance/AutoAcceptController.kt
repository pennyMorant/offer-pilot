package com.overud.acceptance

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.overud.acceptance.AcceptanceGate
import com.overud.acceptance.AcceptanceState
import com.overud.bridge.SheveryBridge
import com.overud.data.repository.AcceptanceRepository
import com.overud.platform.fingerprint
import com.overud.valuation.EligibilityEvaluator
import com.overud.work.WorkSessionController

class AutoAcceptController(
    private val session: WorkSessionController,
    private val repository: AcceptanceRepository,
    private val bridge: SheveryBridge,
    private val scope: CoroutineScope,
    private val mutex: Mutex,
) {
    private var gate = AcceptanceGate()
    private val observer = AcceptanceObserver(repository, session,
        onConfirmed = { gate.occupyConfirmed() }, onCompleted = { gate.userResolved() })
    fun restore() {
        val unresolved = repository.hasUnresolved()
        if (unresolved) gate = AcceptanceGate(AcceptanceState.OUTCOME_UNKNOWN)
        observer.reset()
        session.update { it.copy(pending = unresolved,
            deliveryPhase = if (unresolved) DeliveryPhase.UNCERTAIN else DeliveryPhase.AVAILABLE,
            message = if (unresolved) "有接单结果待核对，请先确认配送状态" else it.message) }
    }
    fun observe(platform: Platform, text: String) {
        observer.observe(platform, text)
    }
    fun resolvePending() {
        scope.launch {
            mutex.withLock {
                repository.resolve()
                gate.userResolved()
                observer.reset()
                session.update { it.copy(pending = false, automatic = false, autoAcceptEnabled = false,
                    deliveryPhase = DeliveryPhase.AVAILABLE, message = "已确认当前无进行中的配送；可重新开启自动接单") }
            }
        }
    }
    fun maybeAccept(offer: Offer, decision: Decision, token: String) {
        val before = session.state.value
        if (!before.automatic || !before.working || before.pending || before.policySaving || !decision.eligible || token.isBlank() ||
            token != fingerprint(offer.platform, offer.rawText)) return
        if (repository.hasUnresolved()) {
            session.update { it.copy(pending = true, automatic = false, autoAcceptEnabled = false,
                deliveryPhase = DeliveryPhase.UNCERTAIN, message = "接单记录待核对") }
            return
        }
        if (!gate.reserve(offer, decision, before.automatic, before.working)) return
        try {
            if (!repository.reserve(offer, decision)) { gate.releaseBeforeDispatch(); return }
            val current = session.state.value
            val rechecked = EligibilityEvaluator.assess(offer, current.policy, System.currentTimeMillis())
            if (!current.automatic || !current.working || current.policySaving ||
                current.policy.version != decision.policyVersion || !rechecked.eligible) {
                repository.update(offer.id, "aborted", "开关或规则已变化")
                gate.releaseBeforeDispatch()
                return
            }
            // Persist before IPC. A process failure can never cause a blind retry.
            session.update { it.copy(pending = true, automatic = false, deliveryPhase = DeliveryPhase.AWAITING_CONFIRMATION) }
            gate.dispatched()
            val result = bridge.service?.accept(token, offer.platform.packageName) ?: "service_disconnected"
            if (result == "dispatched") {
                repository.update(offer.id, "outcome_unknown", "点击已发送，等待平台确认")
                session.update { it.copy(automatic = false, pending = true, message = "接单动作已发出，正在核对平台结果") }
            } else {
                repository.update(offer.id, "aborted", result)
                gate.notDispatched()
                session.releaseDelivery("本次未执行接单：$result")
            }
        } catch (e: Exception) {
            runCatching { repository.update(offer.id, "outcome_unknown", "执行中断：${e.javaClass.simpleName}") }
            session.update { it.copy(automatic = false, autoAcceptEnabled = false, pending = true,
                deliveryPhase = DeliveryPhase.UNCERTAIN, message = "接单结果待核对，请检查平台") }
        }
    }
}
