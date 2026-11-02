package com.overud.data.repository

import com.overud.model.*
import com.overud.data.database.dao.AcceptanceDao
import com.overud.data.database.entity.AttemptRecord

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
        offerId = com.overud.platform.fingerprint(platform, "active-delivery:${System.currentTimeMillis()}")
        this.platform = platform.name
        startedAt = System.currentTimeMillis()
        this.policyVersion = policyVersion
        state = "accepted"
        detail = "active_delivery_screen"
    }) != -1L
}
