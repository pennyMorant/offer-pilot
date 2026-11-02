package com.overud.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_aggregate")
public class DeliveryAggregate {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String platform = "";
    @NonNull public String regionKey = "";
    @NonNull public String zoneId = "";
    public int weekday;
    public int localHour;
    public long hourStart;
    public int measurementVersion;
    public long completedCount;
    public long interruptedCount;
    public long measuredCount;
    public long observedMillisSum;
    public long pairedCount;
    public long pairedObservedMillisSum;
    public long pairedQuotedMillisSum;
    public long longerThanQuoteCount;
}
