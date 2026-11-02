package com.overud.data.repository

import com.overud.analytics.*
import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.RegionWaitingRecord
import com.overud.model.Platform

class WaitingRepository(private val database: OverudDatabase) {
    fun record(bucket: AnalysisBucket, elapsed: Long, completed: Boolean) {
        require(elapsed in 0..1_800_000)
        val id = "waiting|${bucket.platform.name}|${bucket.region}|${bucket.hourStart}|${bucket.zoneId}|${WaitingEvaluator.VERSION}"
        database.runInTransaction {
            val row = database.waiting().find(id) ?: RegionWaitingRecord().apply {
                this.id = id
                platform = bucket.platform.name
                regionKey = bucket.region
                zoneId = bucket.zoneId
                localDate = bucket.localDate
                localHour = bucket.localHour
                weekday = bucket.weekday
                hourStart = bucket.hourStart
                measurementVersion = WaitingEvaluator.VERSION
            }
            if (completed) {
                row.completedCount++
                row.completedMillisSum += elapsed
                row.completedBins = MetricHistogram.adjust(row.completedBins, WaitingEvaluator.BOUNDS, elapsed, 1)
            } else {
                row.interruptedCount++
                row.interruptedMillisSum += elapsed
                row.interruptedBins = MetricHistogram.adjust(row.interruptedBins, WaitingEvaluator.BOUNDS, elapsed, 1)
            }
            database.waiting().save(row)
        }
    }

    fun snapshot(): List<WaitingObservation> {
        val since = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
        return database.waiting().summaries(since).mapNotNull { row ->
            val platform = Platform.entries.find { it.name == row.platform } ?: return@mapNotNull null
            WaitingObservation(platform, row.regionKey, row.zoneId, row.weekday, row.localHour,
                row.measurementVersion, row.completedCount, row.interruptedCount, row.completedMillisSum)
        }
    }
}
