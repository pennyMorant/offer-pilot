package com.overud.data.repository

import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.ExposureRecord
import com.overud.data.database.projection.RegionSummary

class RegionRepository(private val database: OverudDatabase) {
    fun record(exposure: ExposureRecord, duration: Long) {
        database.runInTransaction {
            database.regions().ensureExposure(exposure)
            database.regions().addExposure(exposure.id, duration)
        }
    }
    fun recent(): List<RegionSummary> {
        val since = (System.currentTimeMillis() - 28L * 24 * 3600 * 1000) / 3_600_000 * 3_600_000
        return database.regions().regions(since)
    }
}
