package com.overud.data.database.dao;

public final class QualityQueries {
    private QualityQueries() {}
    public static final String METRICS =
        "SUM(offerCount) AS offerCount, SUM(goodCount) AS goodCount, " +
        "SUM(normalCount) AS normalCount, SUM(lowCount) AS lowCount, " +
        "SUM(unknownCount) AS unknownCount, SUM(ocrCount) AS ocrCount, " +
        "SUM(amountCount) AS amountCount, SUM(amountSum) AS amountSum, " +
        "SUM(distanceCount) AS distanceCount, SUM(distanceSum) AS distanceSum, " +
        "SUM(durationCount) AS durationCount, SUM(durationSum) AS durationSum, " +
        "SUM(perMileCount) AS perMileCount, SUM(perMileMilliSum) AS perMileMilliSum, " +
        "SUM(perHourCount) AS perHourCount, SUM(perHourMilliSum) AS perHourMilliSum ";
}
