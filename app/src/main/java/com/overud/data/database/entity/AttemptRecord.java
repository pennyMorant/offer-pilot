package com.overud.data.database/entity;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "acceptance_attempts")
public class AttemptRecord {
    @PrimaryKey @NonNull public String offerId = "";
    @NonNull public String platform = "";
    public long startedAt;
    public long policyVersion;
    @NonNull public String state = "dispatching";
    @NonNull public String detail = "";
}
