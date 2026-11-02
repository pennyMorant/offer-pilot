package com.overud.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "region_waiting")
public class RegionWaitingRecord {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String platform = "";
    @NonNull public String regionKey = "";
    @NonNull public String zoneId = "";
    @NonNull public String localDate = "";
    public int localHour;
    public int weekday;
    public long hourStart;
    public int measurementVersion;
    public long completedCount;
    public long interruptedCount;
    public long completedMillisSum;
    public long interruptedMillisSum;
    @NonNull public String completedBins = "";
    @NonNull public String interruptedBins = "";
}
