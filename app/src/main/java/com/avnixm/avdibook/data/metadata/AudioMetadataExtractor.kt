package com.avnixm.avdibook.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object AudioMetadataExtractor {
    fun extractDurationMs(context: Context, uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }
}
