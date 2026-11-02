package com.overud.data.mapping

import com.overud.analytics.*
import com.overud.data.database.entity.HourlyQualityRecord

fun AnalysisBucket.emptyRecord() = HourlyQualityRecord().also {
    it.id = id
    it.platform = platform.name
    it.regionKey = region
    it.hourStart = hourStart
    it.zoneId = zoneId
    it.localDate = localDate
    it.localHour = localHour
    it.weekday = weekday
    it.qualityVersion = qualityVersion
}

fun HourlyQualityRecord.adjust(sample: QualitySample, delta: Int) {
    offerCount += delta
    when (sample.quality) {
        OrderQuality.GOOD -> goodCount += delta
        OrderQuality.NORMAL -> normalCount += delta
        OrderQuality.LOW -> lowCount += delta
        OrderQuality.UNKNOWN -> unknownCount += delta
    }
    if (sample.fromOcr) ocrCount += delta
    sample.amountCents?.let { amountCount += delta; amountSum += it * delta }
    sample.distanceMeters?.let { distanceCount += delta; distanceSum += it * delta }
    sample.durationSeconds?.let { durationCount += delta; durationSum += it * delta }
    sample.centsPerMileMilli?.let { perMileCount += delta; perMileMilliSum += it * delta }
    sample.centsPerHourMilli?.let { perHourCount += delta; perHourMilliSum += it * delta }
    amountBins = MetricHistogram.adjust(amountBins, MetricHistogram.AMOUNT_BOUNDS, sample.amountCents, delta)
    mileBins = MetricHistogram.adjust(mileBins, MetricHistogram.MILE_BOUNDS, sample.centsPerMileMilli, delta)
    hourBins = MetricHistogram.adjust(hourBins, MetricHistogram.HOUR_BOUNDS, sample.centsPerHourMilli, delta)
    require(offerCount >= 0 && goodCount >= 0 && normalCount >= 0 && lowCount >= 0 && unknownCount >= 0)
}
