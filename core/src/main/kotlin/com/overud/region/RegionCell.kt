package com.overud.region

import kotlin.math.floor

data class RegionCell(val latitudeIndex: Int, val longitudeIndex: Int) {
    val key: String get() = "$latitudeIndex:$longitudeIndex"
    val centerLatitude get() = (latitudeIndex + 0.5) / 100
    val centerLongitude get() = (longitudeIndex + 0.5) / 100

    fun neighborhood(): Set<String> = (-1..1).flatMap { latitude ->
        (-1..1).map { longitude -> RegionCell(latitudeIndex + latitude, longitudeIndex + longitude).key }
    }.toSet()

    companion object {
        fun parse(key: String): RegionCell? {
            val parts = key.split(':')
            if (parts.size != 2) return null
            val latitude = parts[0].toIntOrNull()?.takeIf { it in -9000..8999 } ?: return null
            val longitude = parts[1].toIntOrNull()?.takeIf { it in -18000..17999 } ?: return null
            return RegionCell(latitude, longitude)
        }
        /** Fixed 0.01-degree cells. Physical width varies with latitude. */
        fun at(latitude: Double, longitude: Double): RegionCell {
            require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
            return RegionCell(floor(latitude * 100).toInt(), floor(longitude * 100).toInt())
        }
    }
}
