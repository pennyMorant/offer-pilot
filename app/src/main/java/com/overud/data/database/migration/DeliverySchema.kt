package com.overud.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DeliverySchema {
    val MIGRATION = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS delivery_aggregate (
                    id TEXT NOT NULL PRIMARY KEY, platform TEXT NOT NULL, regionKey TEXT NOT NULL,
                    zoneId TEXT NOT NULL, weekday INTEGER NOT NULL, localHour INTEGER NOT NULL,
                    hourStart INTEGER NOT NULL, measurementVersion INTEGER NOT NULL,
                    completedCount INTEGER NOT NULL, interruptedCount INTEGER NOT NULL,
                    measuredCount INTEGER NOT NULL, observedMillisSum INTEGER NOT NULL,
                    pairedCount INTEGER NOT NULL, pairedObservedMillisSum INTEGER NOT NULL,
                    pairedQuotedMillisSum INTEGER NOT NULL, longerThanQuoteCount INTEGER NOT NULL)
            """.trimIndent())
        }
    }
}
