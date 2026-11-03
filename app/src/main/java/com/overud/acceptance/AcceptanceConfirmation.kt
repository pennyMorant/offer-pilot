package com.overud.acceptance

import android.os.SystemClock
import com.overud.bridge.IPrivilegedService
import com.overud.bridge.SheveryBridge
import com.overud.model.*
import com.overud.platform.AcceptanceConfirmationPolicy
import com.overud.platform.fingerprint
import com.overud.work.WorkSessionController

/** One transient continuation of an already authorized click; never restored from disk. */
class AcceptanceConfirmation(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
) {
    private data class Pending(
        val offer: Offer, val token: String, val policyVersion: Long,
        val service: IPrivilegedService, val startedAt: Long,
    )
    private var pending: Pending? = null

    fun clear() { pending = null }

    fun start(offer: Offer, token: String, policyVersion: Long, service: IPrivilegedService) {
        pending = Pending(offer, token, policyVersion, service, SystemClock.elapsedRealtime())
    }

    fun observe(page: PlatformPage, text: String, token: String) {
        val context = pending ?: return
        val state = session.state.value
        if (!session.canInteract() || !state.working || !state.autoAcceptEnabled || state.policySaving ||
            state.policy.version != context.policyVersion || !state.pending ||
            state.deliveryPhase != DeliveryPhase.AWAITING_CONFIRMATION ||
            bridge.service?.asBinder() != context.service.asBinder() ||
            SystemClock.elapsedRealtime() - context.startedAt !in 0 until AcceptanceConfirmationPolicy.WINDOW_MILLIS) {
            clear()
            return
        }
        if (page.platform != context.offer.platform || page.source != OfferSource.UI_NODES ||
            page.type != PlatformPageType.ACCEPT_CONFIRMATION) return
        clear() // A confirmation page gets one attempt, including mismatches or failed IPC.
        if (token != fingerprint(page.platform, text) || !AcceptanceConfirmationPolicy.matchesOffer(context.offer, text)) {
            uncertain("确认页与原报价无法完整对应，请手动核对")
            return
        }
        try {
            val latest = session.state.value
            if (!session.canInteract() || !latest.working || !latest.autoAcceptEnabled || latest.policySaving ||
                latest.policy.version != context.policyVersion) return
            val result = context.service.confirmAcceptance(context.token, token, page.platform.packageName,
                SystemClock.elapsedRealtime() + 2000)
            if (result == "dispatched") {
                session.update { if (it.working && it.deliveryPhase == DeliveryPhase.AWAITING_CONFIRMATION)
                    it.copy(message = "已确认同一报价，正在等待平台接单结果") else it }
            } else uncertain("确认动作未完成，请核对平台状态：$result")
        } catch (_: Exception) {
            uncertain("确认动作结果未知，请核对平台配送状态")
        }
    }

    private fun uncertain(message: String) {
        session.update { it.copy(automatic = false, autoAcceptEnabled = false, pending = true,
            deliveryPhase = DeliveryPhase.UNCERTAIN, message = message) }
    }
}
