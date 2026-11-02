package com.overud.data.database.migration

import androidx.sqlite.db.SupportSQLiteDatabase

object AggregateSchema {
    val counters = listOf("offerCount", "goodCount", "normalCount", "lowCount", "unknownCount", "ocrCount",
        "amountCount", "amountSum", "distanceCount", "distanceSum", "durationCount", "durationSum",
        "perMileCount", "perMileMilliSum", "perHourCount", "perHourMilliSum")

    fun create(database: SupportSQLiteDatabase) {
        val counts = counters.joinToString { "`$it` INTEGER NOT NULL" }
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS hourly_quality (
                id TEXT NOT NULL PRIMARY KEY, platform TEXT NOT NULL, regionKey TEXT NOT NULL,
                zoneId TEXT NOT NULL, localDate TEXT NOT NULL, localHour INTEGER NOT NULL,
                weekday INTEGER NOT NULL, hourStart INTEGER NOT NULL, qualityVersion INTEGER NOT NULL,
                $counts, amountBins TEXT NOT NULL, mileBins TEXT NOT NULL, hourBins TEXT NOT NULL)
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS statistics_checkpoint (
                platform TEXT NOT NULL PRIMARY KEY, offerToken TEXT NOT NULL, expiresAt INTEGER NOT NULL)
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS acceptance_state (
                slot TEXT NOT NULL PRIMARY KEY, offerId TEXT NOT NULL, platform TEXT NOT NULL,
                startedAt INTEGER NOT NULL, policyVersion INTEGER NOT NULL,
                state TEXT NOT NULL, detail TEXT NOT NULL)
        """.trimIndent())
    }
}
