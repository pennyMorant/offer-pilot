package com.overud.data.database/entity;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offers")
public class OfferRecord {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String platform = "";
    public long observedAt;
    public Long amountCents;
    public Long distanceMeters;
    public Long durationSeconds;
    public String destination;
    public String originRegion;
    public boolean eligible;
    @NonNull public String explanation = "";
    public long policyVersion;
}
