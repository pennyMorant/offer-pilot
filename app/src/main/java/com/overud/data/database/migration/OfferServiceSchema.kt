package com.overud.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Only adds an aggregate dimension. Existing observations have no recoverable service type. */
object OfferServiceSchema {
    val MIGRATION = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE hourly_quality ADD COLUMN serviceKind TEXT NOT NULL DEFAULT 'UNKNOWN'")
        }
    }
}
