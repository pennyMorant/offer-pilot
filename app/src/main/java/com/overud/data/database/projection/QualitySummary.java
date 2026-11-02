package com.overud.data.database.projection;

import androidx.annotation.NonNull;

public class QualitySummary {
    @NonNull public String platform = "";
    @NonNull public String regionKey = "";
    @NonNull public String zoneId = "";
    public int weekday;
    public int localHour;
    public int qualityVersion;
    public long offerCount;
    public long goodCount;
    public long normalCount;
    public long lowCount;
    public long unknownCount;
    public long ocrCount;
    public long amountCount;
    public long amountSum;
    public long distanceCount;
    public long distanceSum;
    public long durationCount;
    public long durationSum;
    public long perMileCount;
    public long perMileMilliSum;
    public long perHourCount;
    public long perHourMilliSum;
}
