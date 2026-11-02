package com.overud.data.repository

import com.overud.model.*
import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.OfferRecord
import com.overud.data.mapping.toRecord

class OfferRepository(private val database: OverudDatabase) {
    fun recent(): List<OfferRecord> = database.offers().recent()
    fun record(offer: Offer, decision: Decision, region: String?) {
        database.runInTransaction {
            val previous = database.offers().find(offer.id)
            database.offers().save(offer.toRecord(decision, previous, region))
        }
    }
}
