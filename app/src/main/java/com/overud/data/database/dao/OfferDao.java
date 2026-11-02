package com.overud.data.database.dao;

import androidx.room.*;
import java.util.List;
import com.overud.data.database.entity.*;
import com.overud.data.database.projection.RegionSummary;

@Dao
public interface OfferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void save(OfferRecord offer);
    @Query("SELECT * FROM offers ORDER BY observedAt DESC LIMIT 300") List<OfferRecord> recent();
    @Query("SELECT * FROM offers WHERE id = :id") OfferRecord find(String id);
}
