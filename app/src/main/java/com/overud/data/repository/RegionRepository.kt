package com.overud.data.repository

import com.overud.data.database.OverudDatabase
import com.overud.data.database.entity.ExposureRecord

class RegionRepository(private val database: OverudDatabase) {
    fun record(exposure: ExposureRecord, duration: Long) {
        database.runInTransaction {
            database.regions().ensureExposure(exposure)
            database.regions().addExposure(exposure.id, duration)
        }
    }
}
