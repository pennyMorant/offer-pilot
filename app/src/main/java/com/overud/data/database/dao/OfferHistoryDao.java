package com.overud.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.overud.data.database.entity.OfferHistoryRecord;
import java.util.List;

@Dao
public abstract class OfferHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsert(OfferHistoryRecord record);

    @Query("SELECT * FROM offer_history WHERE localDate BETWEEN :startDate AND :endDate ORDER BY observedAt DESC")
    public abstract List<OfferHistoryRecord> byDateRange(String startDate, String endDate);

    @Query("SELECT DISTINCT localDate FROM offer_history ORDER BY localDate DESC LIMIT 90")
    public abstract List<String> recentDates();

    @Query("DELETE FROM offer_history WHERE observedAt < :cutoffMillis")
    public abstract void pruneBefore(long cutoffMillis);

    @Query("DELETE FROM offer_history")
    public abstract void clearAll();
}
