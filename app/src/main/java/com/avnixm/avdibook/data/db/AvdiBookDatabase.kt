package com.avnixm.avdibook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.avnixm.avdibook.data.db.dao.BookDao
import com.avnixm.avdibook.data.db.dao.BookSettingsDao
import com.avnixm.avdibook.data.db.dao.BookmarkDao
import com.avnixm.avdibook.data.db.dao.ChapterDao
import com.avnixm.avdibook.data.db.dao.PlaybackDao
import com.avnixm.avdibook.data.db.dao.TrackDao
import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.db.entity.BookmarkEntity
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity

@Database(
    entities = [
        BookEntity::class,
        TrackEntity::class,
        PlaybackStateEntity::class,
        BookSettingsEntity::class,
        BookmarkEntity::class,
        ChapterEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AvdiBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun trackDao(): TrackDao
    abstract fun playbackDao(): PlaybackDao
    abstract fun bookSettingsDao(): BookSettingsDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_settings` (
                        `bookId` INTEGER NOT NULL,
                        `playbackSpeed` REAL NOT NULL,
                        `skipForwardSec` INTEGER NOT NULL,
                        `skipBackSec` INTEGER NOT NULL,
                        `autoRewindSec` INTEGER NOT NULL,
                        `autoRewindAfterPauseSec` INTEGER NOT NULL,
                        `useLoudnessBoost` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`bookId`),
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `trackId` INTEGER NOT NULL,
                        `positionMs` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_bookmarks_bookId` ON `bookmarks` (`bookId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_bookmarks_createdAt` ON `bookmarks` (`createdAt`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE books ADD COLUMN isMissingSource INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chapters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `trackId` INTEGER,
                        `title` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER,
                        `index` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chapters_bookId` ON `chapters` (`bookId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chapters_bookId_index` ON `chapters` (`bookId`, `index`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN coverArtPath TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM book_settings
                    WHERE playbackSpeed = 1.0
                      AND skipForwardSec = 30
                      AND skipBackSec = 10
                      AND autoRewindSec = 10
                      AND autoRewindAfterPauseSec = 180
                      AND useLoudnessBoost = 0
                    """.trimIndent()
                )
            }
        }

        fun create(context: Context): AvdiBookDatabase {
            return Room.databaseBuilder(
                context,
                AvdiBookDatabase::class.java,
                "avdibook.db"
            )
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .build()
        }
    }
}
