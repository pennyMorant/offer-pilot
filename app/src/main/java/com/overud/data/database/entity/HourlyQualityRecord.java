package com.overud.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hourly_quality")
public class HourlyQualityRecord {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String platform = "";
    @NonNull public String regionKey = "UNKNOWN";
    @NonNull public String zoneId = "UTC";
    @NonNull public String localDate = "";
    public int localHour;
    public int weekday;
    public long hourStart;
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
    @NonNull public String amountBins = "";
    @NonNull public String mileBins = "";
    @NonNull public String hourBins = "";
}
