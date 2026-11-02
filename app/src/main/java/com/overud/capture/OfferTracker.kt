package com.overud.capture

import com.overud.model.*

class OfferTracker {
    private var canonical: String? = null
    private var lastSeenAt = 0L
    private var instanceId: String? = null
    fun reset() { canonical = null; lastSeenAt = 0L; instanceId = null }
    fun observe(offer: Offer, now: Long): Offer {
        if (offer.id != canonical || now - lastSeenAt > 10_000 || instanceId == null) instanceId = "${offer.id}-$now"
        canonical = offer.id
        lastSeenAt = now
        return offer.copy(id = instanceId!!)
    }
}
