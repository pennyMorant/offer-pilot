package io.github.pennymorant.offerpilot.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One opaque deduplication checkpoint per platform, with no quote details. */
@Entity(tableName = "statistics_checkpoint")
public class StatisticsCheckpoint {
    @PrimaryKey @NonNull public String platform = "";
    @NonNull public String offerToken = "";
    public long expiresAt;
}
