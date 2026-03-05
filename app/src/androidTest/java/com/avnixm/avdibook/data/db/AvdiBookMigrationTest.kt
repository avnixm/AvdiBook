package com.avnixm.avdibook.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AvdiBookMigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AvdiBookDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesPhase1Data() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO books (id, title, sourceType, sourceUri, createdAt, lastPlayedAt)
                VALUES (1, 'Test Book', 1, 'files:test', 1, NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO tracks (id, bookId, uri, title, trackIndex, durationMs)
                VALUES (10, 1, 'content://test/track.mp3', 'Track 1', 0, NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO playback_state (bookId, trackId, positionMs, speed, updatedAt)
                VALUES (1, 10, 12000, 1.0, 2)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            AvdiBookDatabase.MIGRATION_1_2
        )

        migrated.query("SELECT COUNT(*) FROM books").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM tracks").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM playback_state").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }
}
