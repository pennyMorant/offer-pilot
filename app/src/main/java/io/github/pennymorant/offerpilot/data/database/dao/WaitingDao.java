package io.github.pennymorant.offerpilot.data.database.dao;

import androidx.room.*;
import io.github.pennymorant.offerpilot.data.database.entity.RegionWaitingRecord;
import io.github.pennymorant.offerpilot.data.database.projection.WaitingSummary;
import java.util.List;

@Dao
public interface WaitingDao {
    @Query("SELECT * FROM region_waiting WHERE id = :id LIMIT 1") RegionWaitingRecord find(String id);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void save(RegionWaitingRecord record);
    @Query("SELECT platform, regionKey, zoneId, weekday, localHour, measurementVersion, " +
        "SUM(completedCount) AS completedCount, SUM(interruptedCount) AS interruptedCount, " +
        "SUM(completedMillisSum) AS completedMillisSum FROM region_waiting WHERE hourStart >= :since " +
        "GROUP BY platform, regionKey, zoneId, weekday, localHour, measurementVersion")
    List<WaitingSummary> summaries(long since);
    @Query("DELETE FROM region_waiting WHERE hourStart < :since") void prune(long since);
}
