package com.avnixm.avdibook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey
    val bookId: Long,
    val trackId: Long,
    val positionMs: Long,
    val speed: Float,
    val updatedAt: Long
)
