package com.javelinco.localmusicplayer.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recent_plays` (
                    `kind` TEXT NOT NULL,
                    `itemId` TEXT NOT NULL,
                    `playedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`kind`, `itemId`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recent_plays_playedAtEpochMs` " +
                    "ON `recent_plays` (`playedAtEpochMs`)",
            )
        }
    }
}
