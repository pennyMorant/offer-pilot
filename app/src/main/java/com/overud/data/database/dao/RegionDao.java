package com.overud.data.database.dao;

import androidx.room.*;
import java.util.List;
import com.overud.data.database.entity.*;
import com.overud.data.database.projection.RegionSummary;

@Dao
public interface RegionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) void ensureExposure(ExposureRecord exposure);
    @Query("UPDATE region_exposure SET exposureMs = exposureMs + :duration WHERE id = :id") void addExposure(String id, long duration);
    @Query("SELECT e.regionKey, e.platform, SUM(e.exposureMs) AS exposureMs, " +
        "(SELECT COUNT(*) FROM offers o WHERE o.originRegion = e.regionKey AND o.platform = e.platform AND o.observedAt >= :since) AS offerCount, " +
        "(SELECT COUNT(*) FROM offers o WHERE o.originRegion = e.regionKey AND o.platform = e.platform AND o.observedAt >= :since AND o.eligible = 1) AS eligibleCount " +
        "FROM region_exposure e WHERE e.bucketStart >= :since GROUP BY e.regionKey, e.platform ORDER BY exposureMs DESC")
    List<RegionSummary> regions(long since);
}
