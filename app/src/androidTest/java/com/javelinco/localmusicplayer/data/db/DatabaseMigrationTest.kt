package com.javelinco.localmusicplayer.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LocalMusicDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationOneToTwoPreservesUserAndLibraryData() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL("INSERT INTO sources VALUES ('source', 'SAF_TREE', 'content://music', 'Music', 1)")
            execSQL("INSERT INTO playlists VALUES ('playlist', 'Road trip', 1, 2)")
            execSQL("INSERT INTO favorites VALUES ('track', 'Song', 'content://song', 3)")
            execSQL("INSERT INTO queue_session VALUES (1, '[\"track\"]', 0, 1234, 4)")
            execSQL("INSERT INTO settings_metadata VALUES ('theme', 'DARK')")
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            2,
            true,
            DatabaseMigrations.MIGRATION_1_2,
        ).use { migrated ->
            assertEquals(1, migrated.rowCount("sources"))
            assertEquals(1, migrated.rowCount("playlists"))
            assertEquals(1, migrated.rowCount("favorites"))
            assertEquals(1, migrated.rowCount("queue_session"))
            assertEquals(1, migrated.rowCount("settings_metadata"))
            assertEquals(0, migrated.rowCount("recent_plays"))
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.rowCount(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val DATABASE_NAME = "migration-1-2"
    }
}
