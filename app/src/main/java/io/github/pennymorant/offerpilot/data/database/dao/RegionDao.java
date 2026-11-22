package io.github.pennymorant.offerpilot.data.database.dao;

import androidx.room.*;
import io.github.pennymorant.offerpilot.data.database.entity.ExposureRecord;
import io.github.pennymorant.offerpilot.data.database.projection.ExposureSummary;
import java.util.List;

@Dao
public interface RegionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) void ensureExposure(ExposureRecord exposure);
    @Query("UPDATE region_exposure SET exposureMs = exposureMs + :duration WHERE id = :id")
    void addExposure(String id, long duration);
    @Query("DELETE FROM region_exposure WHERE bucketStart < :since") void prune(long since);
    @Query("SELECT platform, regionKey, localWeekday, localHour, bucketStart, " +
        "SUM(exposureMs) AS exposureMs FROM region_exposure WHERE bucketStart >= :since " +
        "GROUP BY platform, regionKey, localWeekday, localHour, bucketStart")
    List<ExposureSummary> summaries(long since);
}
