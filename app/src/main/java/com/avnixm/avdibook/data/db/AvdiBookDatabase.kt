package com.avnixm.avdibook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.avnixm.avdibook.data.db.dao.BookDao
import com.avnixm.avdibook.data.db.dao.PlaybackDao
import com.avnixm.avdibook.data.db.dao.TrackDao
import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity

@Database(
    entities = [BookEntity::class, TrackEntity::class, PlaybackStateEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AvdiBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun trackDao(): TrackDao
    abstract fun playbackDao(): PlaybackDao

    companion object {
        fun create(context: Context): AvdiBookDatabase {
            return Room.databaseBuilder(
                context,
                AvdiBookDatabase::class.java,
                "avdibook.db"
            ).build()
        }
    }
}
