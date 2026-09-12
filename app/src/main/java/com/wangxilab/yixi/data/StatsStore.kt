package com.wangxilab.yixi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.wangxilab.yixi.domain.DailyCount
import com.wangxilab.yixi.domain.InterventionResult
import com.wangxilab.yixi.domain.RecentIntervention
import com.wangxilab.yixi.domain.StatisticsSummary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class StatsStore(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    private val appContext = context.applicationContext

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE interventions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                package_name TEXT NOT NULL,
                triggered_at INTEGER NOT NULL,
                resolved_at INTEGER,
                wait_seconds INTEGER NOT NULL DEFAULT 0,
                result TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_interventions_triggered_at ON interventions(triggered_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun begin(packageName: String, triggeredAt: Long = System.currentTimeMillis()): Long {
        val values = ContentValues().apply {
            put("package_name", packageName)
            put("triggered_at", triggeredAt)
        }
        return writableDatabase.insertOrThrow("interventions", null, values)
    }

    @Synchronized
    fun resolve(
        id: Long,
        result: InterventionResult,
        waitSeconds: Int,
        resolvedAt: Long = System.currentTimeMillis(),
    ) {
        val values = ContentValues().apply {
            put("resolved_at", resolvedAt)
            put("wait_seconds", waitSeconds.coerceAtLeast(0))
            put("result", result.name)
        }
        writableDatabase.update("interventions", values, "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun summary(): StatisticsSummary {
        readableDatabase.rawQuery(
            """
            SELECT
                COUNT(*),
                SUM(CASE WHEN result = 'PROCEEDED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN result = 'ABANDONED' THEN 1 ELSE 0 END),
                SUM(wait_seconds)
            FROM interventions
            WHERE result IS NOT NULL AND result != 'FAIL_OPEN'
            """.trimIndent(),
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return StatisticsSummary()
            return StatisticsSummary(
                total = cursor.getInt(0),
                proceeded = cursor.getInt(1),
                abandoned = cursor.getInt(2),
                totalWaitSeconds = cursor.getInt(3),
            )
        }
    }

    @Synchronized
    fun recent(limit: Int = 20): List<RecentIntervention> {
        val packageManager = appContext.packageManager
        return readableDatabase.query(
            "interventions",
            arrayOf("package_name", "triggered_at", "wait_seconds", "result"),
            "result IS NOT NULL AND result != ?",
            arrayOf(InterventionResult.FAIL_OPEN.name),
            null,
            null,
            "triggered_at DESC",
            limit.coerceIn(1, 100).toString(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val packageName = cursor.getString(0)
                    val label = runCatching {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(packageName, 0),
                        ).toString()
                    }.getOrDefault(packageName)
                    val result = runCatching {
                        InterventionResult.valueOf(cursor.getString(3))
                    }.getOrDefault(InterventionResult.TIMEOUT)
                    add(
                        RecentIntervention(
                            appLabel = label,
                            packageName = packageName,
                            triggeredAt = cursor.getLong(1),
                            waitSeconds = cursor.getInt(2),
                            result = result,
                        ),
                    )
                }
            }
        }
    }

    @Synchronized
    fun dailyCounts(days: Int = 7): List<DailyCount> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return (days.coerceIn(1, 14) - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val count = readableDatabase.rawQuery(
                """
                SELECT COUNT(*) FROM interventions
                WHERE triggered_at >= ? AND triggered_at < ?
                  AND result IS NOT NULL AND result != 'FAIL_OPEN'
                """.trimIndent(),
                arrayOf(start.toString(), end.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            DailyCount(
                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
                    .removePrefix("周").removePrefix("星期"),
                count = count,
                isToday = date == today,
            )
        }
    }

    @Synchronized
    fun prune(retentionDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - retentionDays.coerceAtLeast(1) * DAY_MILLIS
        writableDatabase.delete("interventions", "triggered_at < ?", arrayOf(cutoff.toString()))
    }

    @Synchronized
    fun clearAll() {
        writableDatabase.delete("interventions", null, null)
    }

    companion object {
        private const val DATABASE_NAME = "yixi_stats.db"
        private const val DATABASE_VERSION = 1
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
