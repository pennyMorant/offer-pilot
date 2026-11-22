package io.github.pennymorant.offerpilot.data.database;

import io.github.pennymorant.offerpilot.data.database.entity.*;
import io.github.pennymorant.offerpilot.data.database.dao.*;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {HourlyQualityRecord.class, StatisticsCheckpoint.class, AttemptRecord.class, ExposureRecord.class, RegionWaitingRecord.class, DeliveryAggregate.class, OfferHistoryRecord.class}, version = 8, exportSchema = false)
public abstract class OverudDatabase extends RoomDatabase {
    public abstract HourlyQualityDao quality();
    public abstract AcceptanceDao acceptance();
    public abstract RegionDao regions();
    public abstract WaitingDao waiting();
    public abstract DeliveryDao deliveries();
    public abstract OfferHistoryDao offerHistory();
}
