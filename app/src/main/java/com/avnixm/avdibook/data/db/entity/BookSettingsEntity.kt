package com.avnixm.avdibook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_settings",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookSettingsEntity(
    @PrimaryKey
    val bookId: Long,
    val playbackSpeed: Float,
    val skipForwardSec: Int,
    val skipBackSec: Int,
    val autoRewindSec: Int,
    val autoRewindAfterPauseSec: Int,
    val useLoudnessBoost: Boolean,
    val updatedAt: Long
)
