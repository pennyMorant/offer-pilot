package com.overud.domain

import kotlin.math.floor

data class RegionCell(val latitudeIndex: Int, val longitudeIndex: Int) {
    val key: String get() = "$latitudeIndex:$longitudeIndex"
    val centerLatitude get() = (latitudeIndex + 0.5) / 100
    val centerLongitude get() = (longitudeIndex + 0.5) / 100
    companion object {
        /** Fixed 0.01-degree cells. Physical width varies with latitude. */
        fun at(latitude: Double, longitude: Double): RegionCell {
            require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
            return RegionCell(floor(latitude * 100).toInt(), floor(longitude * 100).toInt())
        }
    }
}
