package com.avnixm.avdibook.data.model

data class BackupPayloadV1(
    val schemaVersion: Int,
    val exportedAt: Long,
    val books: List<BackupBook>,
    val tracks: List<BackupTrack>,
    val playbackStates: List<BackupPlaybackState>,
    val settings: List<BackupBookSettings>,
    val bookmarks: List<BackupBookmark>,
    val chapters: List<BackupChapter>
) {
    data class BackupBook(
        val sourceUri: String,
        val title: String,
        val sourceType: Int,
        val createdAt: Long,
        val lastPlayedAt: Long?,
        val isMissingSource: Boolean
    )

    data class BackupTrack(
        val bookSourceUri: String,
        val uri: String,
        val title: String,
        val trackIndex: Int,
        val durationMs: Long?
    )

    data class BackupPlaybackState(
        val bookSourceUri: String,
        val trackUri: String,
        val positionMs: Long,
        val speed: Float,
        val updatedAt: Long
    )

    data class BackupBookSettings(
        val bookSourceUri: String,
        val playbackSpeed: Float,
        val skipForwardSec: Int,
        val skipBackSec: Int,
        val autoRewindSec: Int,
        val autoRewindAfterPauseSec: Int,
        val useLoudnessBoost: Boolean,
        val updatedAt: Long
    )

    data class BackupBookmark(
        val bookSourceUri: String,
        val trackUri: String,
        val positionMs: Long,
        val note: String?,
        val createdAt: Long
    )

    data class BackupChapter(
        val bookSourceUri: String,
        val trackUri: String?,
        val title: String,
        val startMs: Long,
        val endMs: Long?,
        val index: Int
    )
}
