package com.overud.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {OfferRecord.class, AttemptRecord.class, ExposureRecord.class}, version = 1, exportSchema = false)
public abstract class OverudDatabase extends RoomDatabase {
    public abstract OfferDao offers();
}
