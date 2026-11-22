package io.github.pennymorant.offerpilot.data.repository

import io.github.pennymorant.offerpilot.data.database.OverudDatabase
import io.github.pennymorant.offerpilot.data.database.entity.ExposureRecord

class RegionRepository(private val database: OverudDatabase) {
    fun record(exposure: ExposureRecord, duration: Long) {
        database.runInTransaction {
            database.regions().ensureExposure(exposure)
            database.regions().addExposure(exposure.id, duration)
        }
    }
}
