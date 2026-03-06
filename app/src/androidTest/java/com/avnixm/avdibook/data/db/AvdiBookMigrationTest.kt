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

    @Test
    fun migrate2To3_addsChaptersAndMissingFlag() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                """
                INSERT INTO books (id, title, sourceType, sourceUri, createdAt, lastPlayedAt)
                VALUES (1, 'Book V2', 1, 'files:v2', 1, NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO tracks (id, bookId, uri, title, trackIndex, durationMs)
                VALUES (10, 1, 'content://v2/track.mp3', 'Track 1', 0, 1000)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            dbName,
            3,
            true,
            AvdiBookDatabase.MIGRATION_2_3
        )

        migrated.query("SELECT isMissingSource FROM books WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.execSQL(
            """
            INSERT INTO chapters (bookId, trackId, title, startMs, endMs, `index`)
            VALUES (1, 10, 'Chapter 1', 0, 1000, 0)
            """.trimIndent()
        )
        migrated.query("SELECT COUNT(*) FROM chapters WHERE bookId = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun migrate3To4_addsCoverArtPath() {
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                """
                INSERT INTO books (id, title, sourceType, sourceUri, createdAt, lastPlayedAt, isMissingSource)
                VALUES (1, 'Book V3', 1, 'files:v3', 1, NULL, 0)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            dbName,
            4,
            true,
            AvdiBookDatabase.MIGRATION_3_4
        )

        migrated.query("SELECT coverArtPath FROM books WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(null, cursor.getString(0))
        }
    }
}
