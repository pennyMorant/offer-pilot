package com.overud.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object WaitingSchema {
    val MIGRATION = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS region_waiting (
                    id TEXT NOT NULL PRIMARY KEY, platform TEXT NOT NULL, regionKey TEXT NOT NULL,
                    zoneId TEXT NOT NULL, localDate TEXT NOT NULL, localHour INTEGER NOT NULL,
                    weekday INTEGER NOT NULL, hourStart INTEGER NOT NULL, measurementVersion INTEGER NOT NULL,
                    completedCount INTEGER NOT NULL, interruptedCount INTEGER NOT NULL,
                    completedMillisSum INTEGER NOT NULL, interruptedMillisSum INTEGER NOT NULL,
                    completedBins TEXT NOT NULL, interruptedBins TEXT NOT NULL)
            """.trimIndent())
        }
    }
}
