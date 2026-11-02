package com.overud.capture

import com.overud.data.preferences.TrackerCheckpoint
import com.overud.data.preferences.TrackerCheckpointStore
import com.overud.model.*
import com.overud.platform.fingerprint

class OfferTracker(private val store: TrackerCheckpointStore) {
    private val active = mutableMapOf<Platform, TrackerCheckpoint>()
    private val absentSince = mutableMapOf<Platform, Long>()
    private val savedAt = mutableMapOf<Platform, Long>()
    private val latest = mutableMapOf<Platform, Offer>()

    fun reset() { active.clear(); absentSince.clear(); savedAt.clear(); latest.clear() }

    fun observe(offer: Offer, now: Long): Offer {
        absentSince.remove(offer.platform)
        val identity = identity(offer)
        val previous = active[offer.platform] ?: store.read(offer.platform)
        val lastOffer = latest[offer.platform]
        val fillingFields = previous != null && lastOffer != null && now - previous.lastAt in 0..3000 && compatible(lastOffer, offer)
        val same = previous != null && now - previous.lastAt in 0..30_000 && (previous.identity == identity || fillingFields)
        val tracked = if (same) previous!!.copy(identity = identity, lastAt = now)
            else TrackerCheckpoint(identity, fingerprint(offer.platform, "$identity:$now"), now, now)
        active[offer.platform] = tracked
        latest[offer.platform] = offer
        if (!same || now - (savedAt[offer.platform] ?: 0) >= 5000) {
            store.write(offer.platform, tracked)
            savedAt[offer.platform] = now
        }
        return offer.copy(id = tracked.token, firstObservedAt = tracked.firstAt)
    }

    fun absent(platform: Platform, now: Long) {
        val first = absentSince.getOrPut(platform) { now }
        if (now - first < 2000) return
        active.remove(platform)
        latest.remove(platform)
        store.clear(platform)
    }

    private fun identity(offer: Offer): String {
        val route = listOf(offer.currency, offer.merchant, offer.destination, offer.distanceMeters, offer.durationSeconds,
            offer.isStacked, offer.isAddon)
        val fields = if (offer.distanceMeters == null && offer.durationSeconds == null &&
            offer.merchant == null && offer.destination == null) route + offer.amountCents else route
        return fingerprint(offer.platform, fields.joinToString("|"))
    }

    private fun compatible(previous: Offer, next: Offer): Boolean {
        fun matches(a: Any?, b: Any?) = a == null || b == null || a == b
        return previous.currency == next.currency && matches(previous.amountCents, next.amountCents) &&
            matches(previous.merchant, next.merchant) && matches(previous.destination, next.destination) &&
            matches(previous.distanceMeters, next.distanceMeters) && matches(previous.durationSeconds, next.durationSeconds) &&
            previous.isStacked == next.isStacked && previous.isAddon == next.isAddon
    }
}
