package com.overud.analytics

import com.overud.data.database.projection.QualitySummary

object QualitySummaryReducer {
    fun group(rows: List<QualitySummary>, byRegion: Boolean): List<QualitySummary> = rows.groupBy {
        val scope = if (byRegion) it.regionKey else "${it.weekday}:${it.localHour}"
        "${it.platform}|$scope|${it.zoneId}|${it.qualityVersion}|${it.serviceKind}"
    }.values.map { group ->
        val first = group.first()
        QualitySummary().apply {
            platform = first.platform
            serviceKind = first.serviceKind
            regionKey = if (byRegion) first.regionKey else ""
            zoneId = first.zoneId
            weekday = if (byRegion) 0 else first.weekday
            localHour = if (byRegion) 0 else first.localHour
            qualityVersion = first.qualityVersion
            firstObservedHour = group.minOf { it.firstObservedHour }
            for (row in group) {
                offerCount += row.offerCount
                goodCount += row.goodCount
                normalCount += row.normalCount
                lowCount += row.lowCount
                unknownCount += row.unknownCount
                ocrCount += row.ocrCount
                amountCount += row.amountCount
                amountSum += row.amountSum
                distanceCount += row.distanceCount
                distanceSum += row.distanceSum
                durationCount += row.durationCount
                durationSum += row.durationSum
                perMileCount += row.perMileCount
                perMileMilliSum += row.perMileMilliSum
                perHourCount += row.perHourCount
                perHourMilliSum += row.perHourMilliSum
            }
        }
    }
}
