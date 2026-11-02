package com.overud.data.repository

import com.overud.analytics.*
import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.StatisticsCheckpoint
import com.overud.data.mapping.adjust
import com.overud.data.mapping.emptyRecord
import com.overud.model.Offer
import com.overud.model.Platform

class StatisticsRepository(private val database: OverudDatabase) {
    private data class Contribution(val token: String, val bucket: AnalysisBucket, val sample: QualitySample)
    private val current = mutableMapOf<Platform, Contribution>()
    private var prunedAt = 0L

    fun record(offer: Offer, region: String?): Boolean {
        val previous = current[offer.platform]?.takeIf { it.token == offer.id }
        val original = previous?.bucket
        val bucket = if (original == null) AnalysisBucket.at(offer.platform, region, offer.firstObservedAt)
            else if (original.region == AnalysisBucket.UNKNOWN_REGION && region != null) original.copy(region = region)
            else original
        val contribution = Contribution(offer.id, bucket, QualityEvaluator.extract(offer))
        if (contribution == previous) return false
        val dao = database.quality()
        var changed = false
        database.runInTransaction {
            val checkpoint = dao.checkpoint(offer.platform.name)
            val alreadyRecorded = checkpoint?.offerToken == offer.id && checkpoint.expiresAt > System.currentTimeMillis()
            // After a process restart only the token remains; do not invent a reversible sample.
            if (previous == null && alreadyRecorded) return@runInTransaction
            if (previous != null) {
                val old = dao.find(previous.bucket.id) ?: error("Aggregate missing for active observation")
                old.adjust(previous.sample, -1)
                dao.save(old)
            }
            val target = dao.find(bucket.id) ?: bucket.emptyRecord()
            target.adjust(contribution.sample, 1)
            dao.save(target)
            dao.checkpoint(StatisticsCheckpoint().apply {
                platform = offer.platform.name
                offerToken = offer.id
                expiresAt = System.currentTimeMillis() + 24L * 3600 * 1000
            })
            dao.deleteEmpty()
            changed = true
        }
        if (changed) current[offer.platform] = contribution
        prune()
        return changed
    }

    fun snapshot(): StatisticsSnapshot {
        val since = retentionStart()
        val dao = database.quality()
        return StatisticsSnapshot(dao.totals(since), dao.timeSummaries(since), dao.regionSummaries(since), System.currentTimeMillis())
    }

    fun prune() {
        val now = System.currentTimeMillis()
        if (now - prunedAt < 24L * 3600 * 1000) return
        database.runInTransaction {
            database.quality().prune(retentionStart())
            database.regions().prune(retentionStart())
        }
        prunedAt = now
    }

    private fun retentionStart() = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
}
