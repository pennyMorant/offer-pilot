package io.github.pennymorant.offerpilot.acceptance

import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.analytics.DeliveryRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.pennymorant.offerpilot.acceptance.AcceptanceGate
import io.github.pennymorant.offerpilot.acceptance.AcceptanceState
import io.github.pennymorant.offerpilot.bridge.SheveryBridge
import io.github.pennymorant.offerpilot.data.repository.AcceptanceRepository
import io.github.pennymorant.offerpilot.platform.fingerprint
import io.github.pennymorant.offerpilot.work.WorkSessionController
import io.github.pennymorant.offerpilot.work.assessOffer

class AutoAcceptController(
    private val session: WorkSessionController,
    private val repository: AcceptanceRepository,
    private val bridge: SheveryBridge,
    private val scope: CoroutineScope,
    private val mutex: Mutex,
    private val deliveries: DeliveryRecorder,
    onDeliveryCompleted: (Platform) -> Unit = {},
    private val onDeliveryConfirmed: (Platform) -> OfferChannel? = { null },
) {
    private var gate = AcceptanceGate()
    private var confirmationTimeout: Job? = null
    private val confirmation = AcceptanceConfirmation(session, bridge)
    private val observer = AcceptanceObserver(repository, session,
        onConfirmed = { token, platform ->
            confirmation.clear()
            confirmationTimeout?.cancel()
            gate.occupyConfirmed()
            deliveries.confirmed(token, platform)
            val confirmedChannel = onDeliveryConfirmed(platform)
            session.update {
                it.copy(
                    activePlatform = platform,
                    lastCompletedPlatform = null,
                    lastCompletedAt = null,
                    sessionExclusiveAccepted = if (confirmedChannel == OfferChannel.EXCLUSIVE)
                        it.sessionExclusiveAccepted + 1 else it.sessionExclusiveAccepted,
                )
            }
        },
        onCompleted = { token, platform ->
            confirmationTimeout?.cancel()
            gate.userResolved()
            deliveries.completed(token, platform)
            session.update {
                it.copy(activePlatform = null, lastCompletedPlatform = platform, lastCompletedAt = System.currentTimeMillis())
            }
            onDeliveryCompleted(platform)
        })
    fun restore() {
        confirmation.clear()
        confirmationTimeout?.cancel()
        val unresolved = repository.hasUnresolved()
        if (unresolved) gate = AcceptanceGate(AcceptanceState.OUTCOME_UNKNOWN)
        observer.reset()
        session.update { it.copy(pending = unresolved,
            deliveryPhase = if (unresolved) DeliveryPhase.UNCERTAIN else DeliveryPhase.AVAILABLE,
            message = if (unresolved) "有接单结果待核对，请先确认配送状态" else it.message) }
    }
    fun observe(platform: Platform, page: PlatformPage, text: String, token: String) {
        observer.observe(platform, page)
        confirmation.observe(page, text, token)
    }
    fun resolvePending() {
        scope.launch {
            mutex.withLock {
                runCatching { repository.resolve() }.onSuccess {
                    confirmation.clear()
                    confirmationTimeout?.cancel()
                    gate.userResolved()
                    observer.reset()
                    deliveries.resolved()
                    session.update { state -> state.copy(pending = false, automatic = false, autoAcceptEnabled = false,
                        deliveryPhase = DeliveryPhase.AVAILABLE, activePlatform = null, message = "已确认当前无进行中的配送；可重新开启自动接单") }
                }.onFailure {
                    session.update { state -> state.copy(pending = true, automatic = false, autoAcceptEnabled = false,
                        deliveryPhase = DeliveryPhase.UNCERTAIN, message = "订单占用状态未能保存，保持暂停，请稍后重试") }
                }
            }
        }
    }
    fun maybeAccept(offer: Offer, decision: Decision, token: String) {
        val before = session.state.value
        val reviewed = before.assessOffer(offer, System.currentTimeMillis())
        if (!session.canInteract() || !before.automatic || !before.working || before.pending || before.policySaving || !decision.eligible || !reviewed.eligible || token.isBlank() ||
            token != fingerprint(offer.platform, offer.rawText)) return
        if (repository.hasUnresolved()) {
            session.update { it.copy(pending = true, automatic = false, autoAcceptEnabled = false,
                deliveryPhase = DeliveryPhase.UNCERTAIN, message = "接单记录待核对") }
            return
        }
        if (!gate.reserve(offer, reviewed, before.automatic, before.working)) return
        try {
            if (!repository.reserve(offer, reviewed)) { gate.releaseBeforeDispatch(); return }
            val current = session.state.value
            val rechecked = current.assessOffer(offer, System.currentTimeMillis())
            if (!current.automatic || !current.working || current.policySaving ||
                current.policy.version != reviewed.policyVersion || !rechecked.eligible) {
                repository.update(offer.id, "aborted", "开关或规则已变化")
                gate.releaseBeforeDispatch()
                return
            }
            // Persist before IPC. A process failure can never cause a blind retry.
            session.update { it.copy(pending = true, automatic = false, deliveryPhase = DeliveryPhase.AWAITING_CONFIRMATION) }
            val dispatch = session.state.value
            if (!session.canInteract() || !dispatch.autoAcceptEnabled || !dispatch.working || dispatch.policySaving ||
                dispatch.policy.version != reviewed.policyVersion || !dispatch.assessOffer(offer, System.currentTimeMillis()).eligible) {
                repository.update(offer.id, "aborted", "automatic_mode_changed")
                gate.releaseBeforeDispatch()
                session.releaseDelivery("接单动作已停止")
                return
            }
            gate.dispatched()
            deliveries.dispatch(offer)
            val service = bridge.service
            val result = service?.accept(token, offer.platform.packageName,
                android.os.SystemClock.elapsedRealtime() + 2000) ?: "service_disconnected"
            if (result == "dispatched") {
                repository.update(offer.id, "outcome_unknown", "点击已发送，等待平台确认")
                session.update { it.copy(automatic = false, pending = true,
                    message = if (it.working && it.deliveryPhase == DeliveryPhase.AWAITING_CONFIRMATION)
                        "接单动作已发出，正在核对平台结果" else it.message) }
                val awaiting = session.state.value
                if (awaiting.working && awaiting.deliveryPhase == DeliveryPhase.AWAITING_CONFIRMATION) {
                    if (service != null && awaiting.autoAcceptEnabled) confirmation.start(offer, token, reviewed.policyVersion, service)
                    awaitConfirmation()
                }
            } else {
                deliveries.discardCandidate()
                repository.update(offer.id, "aborted", result)
                gate.notDispatched()
                session.releaseDelivery("本次未执行接单：$result")
            }
        } catch (e: Exception) {
            confirmation.clear()
            confirmationTimeout?.cancel()
            deliveries.discardCandidate()
            runCatching { repository.update(offer.id, "outcome_unknown", "执行中断：${e.javaClass.simpleName}") }
            session.update { it.copy(automatic = false, autoAcceptEnabled = false, pending = true,
                deliveryPhase = DeliveryPhase.UNCERTAIN, message = "接单结果待核对，请检查平台") }
        }
    }

    private fun awaitConfirmation() {
        confirmationTimeout?.cancel()
        confirmationTimeout = scope.launch {
            delay(io.github.pennymorant.offerpilot.platform.AcceptanceConfirmationPolicy.WINDOW_MILLIS)
            mutex.withLock {
                val state = session.state.value
                if (gate.state != AcceptanceState.OUTCOME_UNKNOWN || !state.pending ||
                    state.deliveryPhase != DeliveryPhase.AWAITING_CONFIRMATION) return@withLock
                // The persisted slot remains unresolved. A timeout is never evidence of failure.
                confirmation.clear()
                session.update { it.copy(automatic = false, autoAcceptEnabled = false,
                    deliveryPhase = DeliveryPhase.UNCERTAIN,
                    message = "接单确认超时，请检查平台配送状态；自动接单已关闭") }
            }
        }
    }
}
