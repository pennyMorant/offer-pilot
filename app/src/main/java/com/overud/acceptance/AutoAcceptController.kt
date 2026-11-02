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
    private var pendingId: String? = null
    private var pendingPlatform: Platform? = null
    private var gate = AcceptanceGate()
    fun restore() {
        val unresolved = repository.hasUnresolved()
        if (unresolved) gate = AcceptanceGate(AcceptanceState.OUTCOME_UNKNOWN)
        session.update { it.copy(pending = unresolved, message = if (unresolved) "有接单结果待核对，请先确认配送状态" else it.message) }
    }
    fun observe(platform: Platform, text: String) {
        val id = pendingId ?: return
        if (pendingPlatform != platform || !Regex("order accepted|delivery accepted|订单已接受|接单成功", RegexOption.IGNORE_CASE).containsMatchIn(text)) return
        repository.update(id, "accepted", "平台界面显示明确接单成功文本")
        if (gate.state != AcceptanceState.ACCEPTED) gate.confirmed()
        session.update { it.copy(message = "平台已确认接单；配送完成后再恢复自动接单") }
    }
    fun resolvePending() {
        scope.launch {
            mutex.withLock {
                repository.resolve()
                gate.userResolved()
                pendingId = null; pendingPlatform = null
                session.update { it.copy(pending = false, automatic = false, message = "已确认当前无进行中的配送；可重新开启自动接单") }
            }
        }
    }
    fun maybeAccept(offer: Offer, decision: Decision, token: String) {
        val before = session.state.value
        if (!before.automatic || !before.working || before.pending || !decision.eligible || token.isBlank() ||
            token != fingerprint(offer.platform, offer.rawText)) return
        if (repository.hasUnresolved()) {
            session.update { it.copy(pending = true, automatic = false, message = "接单记录待核对") }
            return
        }
        if (!gate.reserve(offer, decision, before.automatic, before.working)) return
        if (!repository.reserve(offer, decision)) { gate.releaseBeforeDispatch(); return }
        try {
            val current = session.state.value
            val rechecked = EligibilityEvaluator.assess(offer, current.policy, System.currentTimeMillis())
            if (!current.automatic || !current.working || current.policy.version != decision.policyVersion || !rechecked.eligible) {
                repository.update(offer.id, "aborted", "开关或规则已变化")
                gate.releaseBeforeDispatch()
                return
            }
            // Persist before IPC. A process failure can never cause a blind retry.
            session.update { it.copy(pending = true) }
            gate.dispatched()
            val result = bridge.service?.accept(token, offer.platform.packageName) ?: "service_disconnected"
            if (result == "dispatched") {
                pendingId = offer.id; pendingPlatform = offer.platform
                repository.update(offer.id, "outcome_unknown", "点击已发送，等待平台确认")
                session.update { it.copy(automatic = false, pending = true, message = "接单动作已发出，正在核对平台结果") }
            } else {
                repository.update(offer.id, "aborted", result)
                gate.notDispatched()
                session.update { it.copy(pending = false, message = "本次未执行接单：$result") }
            }
        } catch (e: Exception) {
            repository.update(offer.id, "outcome_unknown", "执行中断：${e.javaClass.simpleName}")
            session.update { it.copy(automatic = false, pending = true, message = "接单结果待核对，请检查平台") }
        }
    }
}
