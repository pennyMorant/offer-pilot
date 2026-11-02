package com.overud.data.database/entity;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "region_exposure")
public class ExposureRecord {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String regionKey = "";
    @NonNull public String platform = "";
    public long bucketStart;
    public int localHour;
    public int localWeekday;
    public long exposureMs;
}
