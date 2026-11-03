package com.overud.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds only an aggregate dimension; older rows cannot be assigned a service type. */
object DeliveryServiceSchema {
    val MIGRATION = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE delivery_aggregate ADD COLUMN serviceKind TEXT NOT NULL DEFAULT 'UNKNOWN'",
            )
        }
    }
}
