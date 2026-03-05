package com.avnixm.avdibook.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["sourceUri"])]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceType: Int,
    val sourceUri: String,
    val createdAt: Long,
    val lastPlayedAt: Long?
)
