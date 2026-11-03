package com.overud.data.database.dao;

import androidx.room.*;
import java.util.List;
import com.overud.data.database.entity.HourlyQualityRecord;
import com.overud.data.database.entity.StatisticsCheckpoint;
import com.overud.data.database.projection.QualitySummary;

@Dao
public interface HourlyQualityDao {
    @Query("SELECT * FROM hourly_quality WHERE id = :id") HourlyQualityRecord find(String id);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void save(HourlyQualityRecord record);
    @Query("DELETE FROM hourly_quality WHERE offerCount = 0") void deleteEmpty();
    @Query("DELETE FROM hourly_quality WHERE hourStart < :since") void prune(long since);
    @Query("SELECT * FROM statistics_checkpoint WHERE platform = :platform") StatisticsCheckpoint checkpoint(String platform);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void checkpoint(StatisticsCheckpoint checkpoint);
    @Query("SELECT platform, regionKey, zoneId, weekday, localHour, qualityVersion, serviceKind, " + QualityQueries.METRICS +
        "FROM hourly_quality WHERE hourStart >= :since " +
        "GROUP BY platform, regionKey, zoneId, weekday, localHour, qualityVersion, serviceKind ORDER BY weekday, localHour, platform")
    List<QualitySummary> timeSummaries(long since);
    @Query("SELECT platform, regionKey, zoneId, 0 AS weekday, 0 AS localHour, qualityVersion, serviceKind, " + QualityQueries.METRICS +
        "FROM hourly_quality WHERE hourStart >= :since " +
        "GROUP BY platform, regionKey, zoneId, qualityVersion, serviceKind ORDER BY offerCount DESC")
    List<QualitySummary> regionSummaries(long since);
    @Query("SELECT '' AS platform, '' AS regionKey, '' AS zoneId, 0 AS weekday, 0 AS localHour, qualityVersion, serviceKind, " + QualityQueries.METRICS +
        "FROM hourly_quality WHERE hourStart >= :since GROUP BY qualityVersion, serviceKind")
    List<QualitySummary> totals(long since);
}
