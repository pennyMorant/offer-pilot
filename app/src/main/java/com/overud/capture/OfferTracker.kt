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
        val tracked = if (same) previous.copy(identity = identity, lastAt = now)
            else TrackerCheckpoint(identity, fingerprint(offer.platform, "$identity:$now"), now, now)
        val merged = if (same && lastOffer != null) {
            offer.copy(
                id = tracked.token,
                firstObservedAt = tracked.firstAt,
                channel = if (offer.channel != OfferChannel.UNKNOWN) offer.channel else lastOffer.channel,
                preferredTag = offer.preferredTag || lastOffer.preferredTag,
                itemCount = offer.itemCount ?: lastOffer.itemCount,
            )
        } else {
            offer.copy(id = tracked.token, firstObservedAt = tracked.firstAt)
        }
        active[offer.platform] = tracked
        latest[offer.platform] = merged
        if (!same || now - (savedAt[offer.platform] ?: 0) >= 5000) {
            store.write(offer.platform, tracked)
            savedAt[offer.platform] = now
        }
        return merged
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
            offer.isStacked, offer.isAddon, offer.serviceKind, offer.pickupAddress)
        val fields = if (offer.distanceMeters == null && offer.durationSeconds == null &&
            offer.merchant == null && offer.destination == null) route + offer.amountCents else route
        return fingerprint(offer.platform, fields.joinToString("|"))
    }

    private fun compatible(previous: Offer, next: Offer): Boolean {
        fun matches(a: Any?, b: Any?) = a == null || b == null || a == b
        val sameKind = previous.serviceKind == OfferService.UNKNOWN || next.serviceKind == OfferService.UNKNOWN ||
            previous.serviceKind == next.serviceKind
        val sameChannel = previous.channel == OfferChannel.UNKNOWN || next.channel == OfferChannel.UNKNOWN ||
            previous.channel == next.channel
        return sameKind && sameChannel && matches(previous.pickupAddress, next.pickupAddress) &&
            matches(previous.itemCount, next.itemCount) &&
            previous.currency == next.currency && matches(previous.amountCents, next.amountCents) &&
            matches(previous.merchant, next.merchant) && matches(previous.destination, next.destination) &&
            matches(previous.distanceMeters, next.distanceMeters) && matches(previous.durationSeconds, next.durationSeconds) &&
            previous.isStacked == next.isStacked && previous.isAddon == next.isAddon
    }
}
