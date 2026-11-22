package io.github.pennymorant.offerpilot.data.repository

import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.data.database.dao.AcceptanceDao
import io.github.pennymorant.offerpilot.data.database.entity.AttemptRecord

class AcceptanceRepository(private val dao: AcceptanceDao) {
    fun active() = dao.active()
    fun hasUnresolved() = dao.unresolvedCount() > 0
    fun reserve(offer: Offer, decision: Decision): Boolean = dao.reserve(AttemptRecord().apply {
        offerId = offer.id
        platform = offer.platform.name
        startedAt = System.currentTimeMillis()
        policyVersion = decision.policyVersion
    }) != -1L
    fun update(id: String, state: String, detail: String) = dao.updateAttempt(id, state, detail)
    fun resolve() = dao.resolve()
    fun adoptActive(platform: Platform, policyVersion: Long): Boolean = dao.reserve(AttemptRecord().apply {
        offerId = io.github.pennymorant.offerpilot.platform.fingerprint(platform, "active-delivery:${System.currentTimeMillis()}")
        this.platform = platform.name
        startedAt = System.currentTimeMillis()
        this.policyVersion = policyVersion
        state = "accepted"
        detail = "active_delivery_screen"
    }) != -1L
}
