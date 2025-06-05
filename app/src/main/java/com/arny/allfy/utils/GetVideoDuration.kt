package com.arny.allfy.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

fun getVideoDuration(uri: Uri, context: Context): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: 0L
        retriever.release()
        duration
    } catch (e: Exception) {
        0L
    }
}