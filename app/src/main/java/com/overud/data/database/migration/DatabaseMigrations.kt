package com.overud.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.overud.analytics.MetricHistogram

object DatabaseMigrations {
    val TO_AGGREGATES = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AggregateSchema.create(database)
            summarizeLegacy(database)
            database.execSQL("""
                INSERT INTO acceptance_state (slot, offerId, platform, startedAt, policyVersion, state, detail)
                SELECT 'active', offerId, platform, startedAt, policyVersion, state, ''
                FROM acceptance_attempts WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')
                ORDER BY startedAt DESC LIMIT 1
            """.trimIndent())
            database.execSQL("DROP TABLE offers")
            database.execSQL("DROP TABLE acceptance_attempts")
        }
    }

    private fun summarizeLegacy(database: SupportSQLiteDatabase) {
        val hour = "(observedAt / 3600000) * 3600000"
        val region = "COALESCE(originRegion, 'UNKNOWN')"
        val histogram = legacyAmountHistogram()
        val sums = AggregateSchema.counters.map { column ->
            when (column) {
                "offerCount", "unknownCount" -> "COUNT(*)"
                "amountCount" -> "SUM(CASE WHEN amountCents > 0 THEN 1 ELSE 0 END)"
                "amountSum" -> "SUM(CASE WHEN amountCents > 0 THEN amountCents ELSE 0 END)"
                else -> "0"
            }
        }.joinToString()
        database.execSQL("""
            INSERT INTO hourly_quality (id, platform, regionKey, zoneId, localDate, localHour,
                weekday, hourStart, qualityVersion, ${AggregateSchema.counters.joinToString()}, amountBins, mileBins, hourBins)
            SELECT 'legacy|' || platform || '|' || $region || '|' || ($hour),
                platform, $region, 'UTC', strftime('%Y-%m-%d', observedAt / 1000, 'unixepoch'),
                CAST(strftime('%H', observedAt / 1000, 'unixepoch') AS INTEGER),
                ((CAST(strftime('%w', observedAt / 1000, 'unixepoch') AS INTEGER) + 6) % 7) + 1,
                $hour, 0, $sums, $histogram, '', ''
            FROM offers WHERE observedAt >= ? GROUP BY platform, $region, $hour
        """.trimIndent(), arrayOf(System.currentTimeMillis() - 90L * 24 * 3600 * 1000))
    }

    private fun legacyAmountHistogram(): String {
        val bounds = MetricHistogram.AMOUNT_BOUNDS
        return (0..bounds.size).joinToString(" || ',' || ") { index ->
            val lower = if (index == 0) "amountCents > 0" else "amountCents >= ${bounds[index - 1]}"
            val upper = if (index < bounds.size) " AND amountCents < ${bounds[index]}" else ""
            "SUM(CASE WHEN $lower$upper THEN 1 ELSE 0 END)"
        }
    }
}
