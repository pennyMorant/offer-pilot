package io.github.pennymorant.offerpilot.data.database.dao;

import androidx.room.*;
import io.github.pennymorant.offerpilot.data.database.entity.DeliveryAggregate;
import io.github.pennymorant.offerpilot.data.database.projection.DeliverySummary;
import java.util.List;

@Dao
public interface DeliveryDao {
    @Query("SELECT * FROM delivery_aggregate WHERE id = :id LIMIT 1") DeliveryAggregate find(String id);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void save(DeliveryAggregate record);
    @Query("SELECT platform, serviceKind, regionKey, zoneId, weekday, localHour, measurementVersion, " +
        "SUM(completedCount) AS completedCount, SUM(interruptedCount) AS interruptedCount, " +
        "SUM(measuredCount) AS measuredCount, SUM(observedMillisSum) AS observedMillisSum, " +
        "SUM(pairedCount) AS pairedCount, SUM(pairedObservedMillisSum) AS pairedObservedMillisSum, " +
        "SUM(pairedQuotedMillisSum) AS pairedQuotedMillisSum, SUM(longerThanQuoteCount) AS longerThanQuoteCount " +
        "FROM delivery_aggregate WHERE hourStart >= :since " +
        "GROUP BY platform, serviceKind, regionKey, zoneId, weekday, localHour, measurementVersion")
    List<DeliverySummary> summaries(long since);
    @Query("DELETE FROM delivery_aggregate WHERE hourStart < :since") void prune(long since);
}
