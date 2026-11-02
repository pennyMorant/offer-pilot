package com.overud.data;

import androidx.room.*;
import java.util.List;

@Dao
public interface OfferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void save(OfferRecord offer);
    @Query("SELECT * FROM offers ORDER BY observedAt DESC LIMIT 300") List<OfferRecord> recent();
    @Query("SELECT * FROM offers WHERE id = :id") OfferRecord find(String id);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long reserve(AttemptRecord attempt);
    @Query("UPDATE acceptance_attempts SET state = :state, detail = :detail WHERE offerId = :id")
    void updateAttempt(String id, String state, String detail);
    @Query("SELECT COUNT(*) FROM acceptance_attempts WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')") int unresolvedCount();
    @Query("UPDATE acceptance_attempts SET state = 'user_resolved' WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')") void resolve();
    @Insert(onConflict = OnConflictStrategy.IGNORE) void ensureExposure(ExposureRecord exposure);
    @Query("UPDATE region_exposure SET exposureMs = exposureMs + :duration WHERE id = :id") void addExposure(String id, long duration);
    @Query("SELECT e.regionKey, e.platform, SUM(e.exposureMs) AS exposureMs, " +
        "(SELECT COUNT(*) FROM offers o WHERE o.originRegion = e.regionKey AND o.platform = e.platform AND o.observedAt >= :since) AS offerCount, " +
        "(SELECT COUNT(*) FROM offers o WHERE o.originRegion = e.regionKey AND o.platform = e.platform AND o.observedAt >= :since AND o.eligible = 1) AS eligibleCount " +
        "FROM region_exposure e WHERE e.bucketStart >= :since GROUP BY e.regionKey, e.platform ORDER BY exposureMs DESC")
    List<RegionSummary> regions(long since);
}
