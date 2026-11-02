package com.overud.region

import com.overud.model.*

object DestinationCoordinates {
    private val plain = Regex("^([+-]?\\d{1,2}\\.\\d{3,})\\s*,\\s*([+-]?\\d{1,3}\\.\\d{3,})$")
    private val geo = Regex("^geo:([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:[?;].*)?$", RegexOption.IGNORE_CASE)

    fun parse(value: String): ResolvedDestination? {
        val match = plain.matchEntire(value.trim()) ?: geo.matchEntire(value.trim()) ?: return null
        val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return ResolvedDestination(DestinationResolution.RESOLVED, RegionCell.at(latitude, longitude).key,
            DestinationPrecision.COORDINATE)
    }
}
