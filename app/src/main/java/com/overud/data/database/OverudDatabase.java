package com.overud.data.database;

import com.overud.data.database.entity.*;
import com.overud.data.database.dao.*;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {HourlyQualityRecord.class, StatisticsCheckpoint.class, AttemptRecord.class, ExposureRecord.class}, version = 2, exportSchema = false)
public abstract class OverudDatabase extends RoomDatabase {
    public abstract HourlyQualityDao quality();
    public abstract AcceptanceDao acceptance();
    public abstract RegionDao regions();
}
