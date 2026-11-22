package io.github.pennymorant.offerpilot.analytics

import io.github.pennymorant.offerpilot.model.*
import android.location.Location
import io.github.pennymorant.offerpilot.region.RegionCell
import io.github.pennymorant.offerpilot.data.database.entity.ExposureRecord
import io.github.pennymorant.offerpilot.data.repository.RegionRepository
import io.github.pennymorant.offerpilot.work.WorkSessionController

class RegionRecorder(private val session: WorkSessionController, private val repository: RegionRepository) {
    @Volatile private var lastLocation: Location? = null
    private var exposureKey: String? = null
    private var exposureAt = 0L
    fun clear() {
        lastLocation = null
        exposureKey = null
        exposureAt = 0L
        session.update { it.copy(currentRegion = null) }
    }
    fun location(location: Location) {
        if (!session.state.value.working) return
        lastLocation = Location(location)
        session.update { it.copy(currentRegion = usableRegion()) }
    }
    fun usableRegion(): String? {
        val location = lastLocation ?: return null
        if (System.currentTimeMillis() - location.time !in 0..120_000 || !location.hasAccuracy() || location.accuracy > 300) return null
        return RegionCell.at(location.latitude, location.longitude).key
    }
    fun trackExposure(platform: Platform, now: Long) {
        val region = usableRegion()
        val key = if (region != null && !session.state.value.pending) "$region:${platform.name}" else null
        val elapsed = now - exposureAt
        if (key != null && key == exposureKey && elapsed in 1..5000) {
            val bucket = now / 3_600_000 * 3_600_000
            val id = "$key:$bucket"
            val local = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault())
            repository.record(ExposureRecord().apply {
                this.id = id; regionKey = region!!; this.platform = platform.name; bucketStart = bucket
                localHour = local.hour; localWeekday = local.dayOfWeek.value
            }, elapsed)
        }
        exposureAt = now; exposureKey = key
    }
}
