package com.overud.data.repository

import com.overud.model.*
import com.overud.data.database.dao.AcceptanceDao
import com.overud.data.database.entity.AttemptRecord

class AcceptanceRepository(private val dao: AcceptanceDao) {
    fun hasUnresolved() = dao.unresolvedCount() > 0
    fun reserve(offer: Offer, decision: Decision): Boolean = dao.reserve(AttemptRecord().apply {
        offerId = offer.id
        platform = offer.platform.name
        startedAt = System.currentTimeMillis()
        policyVersion = decision.policyVersion
    }) != -1L
    fun update(id: String, state: String, detail: String) = dao.updateAttempt(id, state, detail)
    fun resolve() = dao.resolve()
}
