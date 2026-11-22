package io.github.pennymorant.offerpilot.data.repository

import io.github.pennymorant.offerpilot.analytics.AnalysisBucket
import io.github.pennymorant.offerpilot.analytics.DeliveryMeasurement
import io.github.pennymorant.offerpilot.data.database.OverudDatabase
import io.github.pennymorant.offerpilot.data.database.entity.DeliveryAggregate

class DeliveryRepository(private val database: OverudDatabase) {
    fun record(bucket: AnalysisBucket, completed: Boolean, observed: Long?, quoted: Long?) {
        val duration = observed?.takeIf { completed && it in 1..DeliveryMeasurement.MAX_MILLIS }
        val quote = quoted?.takeIf { duration != null && it in 1..DeliveryMeasurement.MAX_MILLIS }
        val key = "delivery|${bucket.platform.name}|${bucket.serviceKind.name}|${bucket.region}|" +
            "${bucket.hourStart}|${bucket.zoneId}|${DeliveryMeasurement.VERSION}"
        database.runInTransaction {
            val row = database.deliveries().find(key) ?: DeliveryAggregate().apply {
                id = key
                platform = bucket.platform.name
                serviceKind = bucket.serviceKind.name
                regionKey = bucket.region
                zoneId = bucket.zoneId
                weekday = bucket.weekday
                localHour = bucket.localHour
                hourStart = bucket.hourStart
                measurementVersion = DeliveryMeasurement.VERSION
            }
            if (completed) row.completedCount++ else row.interruptedCount++
            if (duration != null) {
                row.measuredCount++
                row.observedMillisSum += duration
                if (quote != null) {
                    row.pairedCount++
                    row.pairedObservedMillisSum += duration
                    row.pairedQuotedMillisSum += quote
                    if (duration > quote) row.longerThanQuoteCount++
                }
            }
            database.deliveries().save(row)
        }
    }

    fun snapshot() = database.deliveries().summaries(System.currentTimeMillis() - 90L * 24 * 3600 * 1000)
}
