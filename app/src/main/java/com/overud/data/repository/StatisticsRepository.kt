package com.overud.data.repository

import com.overud.analytics.*
import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.StatisticsCheckpoint
import com.overud.data.mapping.adjust
import com.overud.data.mapping.emptyRecord
import com.overud.model.Offer
import com.overud.model.Platform
import java.util.concurrent.atomic.AtomicLong

class StatisticsRepository(private val database: OverudDatabase) {
    private data class Contribution(val token: String, val bucket: AnalysisBucket, val sample: QualitySample)
    private val current = mutableMapOf<Platform, Contribution>()
    private var prunedAt = 0L
    private val snapshotRevision = AtomicLong()

    fun record(offer: Offer, region: String?): Boolean {
        if (offer.currency != "USD") return false
        val amount = offer.amountCents
        if (amount == null || amount <= 0) return false
        val previous = current[offer.platform]?.takeIf { it.token == offer.id }
        val original = previous?.bucket
        val locatedBucket = if (original == null) AnalysisBucket.at(offer.platform, region, offer.firstObservedAt)
            else if (original.region == AnalysisBucket.UNKNOWN_REGION && region != null) original.copy(region = region)
            else original
        val kind = offer.serviceKind.takeUnless { it == com.overud.model.OfferService.UNKNOWN }
            ?: locatedBucket.serviceKind
        val bucket = locatedBucket.copy(serviceKind = kind)
        val contribution = Contribution(offer.id, bucket, QualityEvaluator.extract(offer, previous?.sample))
        if (contribution == previous) return false
        val dao = database.quality()
        var changed = false
        database.runInTransaction {
            val checkpoint = dao.checkpoint(offer.platform.name)
            val alreadyRecorded = checkpoint != null && checkpoint.offerToken == offer.id &&
                checkpoint.expiresAt > System.currentTimeMillis()
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
        return StatisticsSnapshot(dao.totals(since), dao.timeSummaries(since), dao.regionSummaries(since), System.currentTimeMillis(),
            revision = snapshotRevision.incrementAndGet())
    }

    fun prune(): Boolean {
        val now = System.currentTimeMillis()
        if (now - prunedAt in 0 until 24L * 3600 * 1000) return false
        database.runInTransaction {
            database.quality().prune(retentionStart())
            database.regions().prune(retentionStart())
            database.waiting().prune(retentionStart())
            database.deliveries().prune(retentionStart())
        }
        prunedAt = now
        return true
    }

    private fun retentionStart() = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
}
