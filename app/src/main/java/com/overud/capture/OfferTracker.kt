package com.overud.capture

import com.overud.data.preferences.TrackerCheckpoint
import com.overud.data.preferences.TrackerCheckpointStore
import com.overud.model.*
import com.overud.platform.fingerprint

class OfferTracker(private val store: TrackerCheckpointStore) {
    private val active = mutableMapOf<Platform, TrackerCheckpoint>()
    private val absentSince = mutableMapOf<Platform, Long>()
    private val savedAt = mutableMapOf<Platform, Long>()

    fun reset() { active.clear(); absentSince.clear(); savedAt.clear() }

    fun observe(offer: Offer, now: Long): Offer {
        absentSince.remove(offer.platform)
        val identity = identity(offer)
        val previous = active[offer.platform] ?: store.read(offer.platform)
        val same = previous != null && previous.identity == identity && now - previous.lastAt in 0..30_000
        val tracked = if (same) previous!!.copy(lastAt = now)
            else TrackerCheckpoint(identity, fingerprint(offer.platform, "$identity:$now"), now, now)
        active[offer.platform] = tracked
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
        store.clear(platform)
    }

    private fun identity(offer: Offer): String {
        val route = listOf(offer.merchant, offer.destination, offer.distanceMeters, offer.durationSeconds,
            offer.isStacked, offer.isAddon)
        val fields = if (offer.distanceMeters == null && offer.durationSeconds == null &&
            offer.merchant == null && offer.destination == null) route + offer.amountCents else route
        return fingerprint(offer.platform, fields.joinToString("|"))
    }
}
