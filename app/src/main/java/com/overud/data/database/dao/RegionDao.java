package com.overud.data.database.dao;

import androidx.room.*;
import com.overud.data.database.entity.ExposureRecord;

@Dao
public interface RegionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) void ensureExposure(ExposureRecord exposure);
    @Query("UPDATE region_exposure SET exposureMs = exposureMs + :duration WHERE id = :id")
    void addExposure(String id, long duration);
    @Query("DELETE FROM region_exposure WHERE bucketStart < :since") void prune(long since);
}
