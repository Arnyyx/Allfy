package com.arny.allfy.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)
        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}

suspend fun getVideoThumbnailFromFirebase(context: Context, firebaseUrl: String): Bitmap? {
    val storage = Firebase.storage
    val storageRef = storage.getReferenceFromUrl(firebaseUrl)
    val tempFile = withContext(Dispatchers.IO) {
        File.createTempFile("video", "tmp", context.cacheDir)
    }

    return try {
        val downloadTask = storageRef.getFile(tempFile)
        downloadTask.await()
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(tempFile.path)
        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            .also { retriever.release() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        tempFile.delete()
    }
}