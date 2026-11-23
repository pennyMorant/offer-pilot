package io.github.pennymorant.offerpilot.acceptance

import io.github.pennymorant.offerpilot.data.repository.OfferHistoryRepository
import io.github.pennymorant.offerpilot.model.Offer
import io.github.pennymorant.offerpilot.model.OfferChannel
import io.github.pennymorant.offerpilot.model.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Attributes each observed offer to ACCEPTED or DECLINED based on subsequent platform screens. */
class OfferOutcomeTracker(
    private val history: OfferHistoryRepository,
    private val scope: CoroutineScope,
) {
    data class PendingOffer(
        val offerId: String,
        val channel: OfferChannel,
        val lastSeenAt: Long,
    )

    private val lock = Any()
    private val pending = mutableMapOf<Platform, PendingOffer>()

    fun onOfferSeen(offer: Offer, now: Long) {
        val replaced: PendingOffer?
        synchronized(lock) {
            val prev = pending[offer.platform]
            replaced = prev?.takeIf { it.offerId != offer.id }
            pending[offer.platform] = PendingOffer(offer.id, offer.channel, now)
        }
        replaced?.let { markDeclinedAsync(it.offerId) }
    }

    fun onDeliveryConfirmed(platform: Platform, now: Long): PendingOffer? {
        val matched = synchronized(lock) {
            val item = pending[platform] ?: return@synchronized null
            pending.remove(platform)
            item.takeIf { now - it.lastSeenAt in 0..ACCEPT_WINDOW_MS }
        }
        matched?.let { markAcceptedAsync(it.offerId) }
        return matched
    }

    fun onNoOfferFrame(platform: Platform, deliveryActive: Boolean, now: Long) {
        if (deliveryActive) return
        val expired = synchronized(lock) {
            val item = pending[platform] ?: return@synchronized null
            if (now - item.lastSeenAt >= DECLINE_GRACE_MS) pending.remove(platform) else null
        }
        expired?.let { markDeclinedAsync(it.offerId) }
    }

    fun onExplicitDecline(offer: Offer) {
        synchronized(lock) {
            if (pending[offer.platform]?.offerId == offer.id) pending.remove(offer.platform)
        }
        markDeclinedAsync(offer.id)
    }

    fun onIdle(now: Long, maxAgeMs: Long = ACCEPT_WINDOW_MS) {
        val expired = synchronized(lock) {
            val stale = pending.entries.filter { now - it.value.lastSeenAt >= maxAgeMs }
            stale.forEach { pending.remove(it.key) }
            stale.map { it.value.offerId }
        }
        expired.forEach(::markDeclinedAsync)
    }

    private fun markAcceptedAsync(offerId: String) {
        scope.launch(Dispatchers.IO) { runCatching { history.markAccepted(offerId) } }
    }

    private fun markDeclinedAsync(offerId: String) {
        scope.launch(Dispatchers.IO) { runCatching { history.markDeclined(offerId) } }
    }

    companion object {
        private const val DECLINE_GRACE_MS = 4_000L
        private const val ACCEPT_WINDOW_MS = 45_000L
    }
}
