package com.arny.allfy.data.remote

import android.content.Context
import android.net.Uri
import com.arny.allfy.domain.model.Story
import com.arny.allfy.domain.repository.StoryRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.getVideoDuration
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context
) : StoryRepository {

    override suspend fun uploadStory(story: Story, mediaUri: Uri): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            if (story.duration <= 0) {
                emit(Response.Error("Story duration must be greater than 0"))
                return@flow
            }

            val storyID = firestore.collection(Constants.COLLECTION_NAME_STORIES).document().id
            val mimeType = context.contentResolver.getType(mediaUri)
            val extension = if (mimeType?.startsWith("image/") == true) ".jpg" else ".mp4"
            val fileName = "${System.currentTimeMillis()}$extension"

            val mediaType = if (mimeType?.startsWith("image/") == true) "image" else "video"
            if (mediaType == "video") {
                val videoDuration = getVideoDuration(mediaUri, context)
                if (videoDuration > story.maxVideoDuration!!) {
                    emit(Response.Error("Video exceeds maximum duration of ${story.maxVideoDuration / 1000} seconds"))
                    return@flow
                }
            }

            val storageRef =
                storage.reference.child("${Constants.COLLECTION_NAME_STORIES}/${story.userID}/$storyID/$fileName")
            storageRef.putFile(mediaUri).await()
            val mediaUrl = storageRef.downloadUrl.await().toString()

            val newStory = story.copy(
                storyID = storyID,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                imageDuration = if (mediaType == "image") story.imageDuration else null,
                maxVideoDuration = if (mediaType == "video") story.maxVideoDuration else null,
                timestamp = Timestamp.now()
            )

            firestore.collection(Constants.COLLECTION_NAME_STORIES)
                .document(storyID)
                .set(newStory)
                .await()

            firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(story.userID)
                .collection("stories")
                .document(storyID)
                .set(mapOf("storyID" to storyID, "timestamp" to newStory.timestamp))
                .await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun getUserStories(userID: String): Flow<Response<List<Story>>> = flow {
        emit(Response.Loading)
        try {
            val storyRefs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userID)
                .collection("stories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val storyIds = storyRefs.documents.mapNotNull { it.getString("storyID") }

            if (storyIds.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }

            val stories = firestore.collection(Constants.COLLECTION_NAME_STORIES)
                .whereIn("storyID", storyIds)
                .get()
                .await()
                .toObjects(Story::class.java)
                .filter { story ->
                    val expiryTime = story.timestamp.toDate().time + (story.duration * 1000)
                    System.currentTimeMillis() <= expiryTime
                }

            emit(Response.Success(stories))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun getStoriesByUserIds(userIds: List<String>): Flow<Response<List<Story>>> =
        flow {
            emit(Response.Loading)
            try {
                if (userIds.isEmpty()) {
                    emit(Response.Success(emptyList()))
                    return@flow
                }

                val stories = mutableListOf<Story>()
                userIds.chunked(10).forEach { batch ->
                    val batchStories = firestore.collection(Constants.COLLECTION_NAME_STORIES)
                        .whereIn("userID", batch)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .await()
                        .toObjects(Story::class.java)
                        .filter { story ->
                            val expiryTime = story.timestamp.toDate().time + (story.duration * 1000)
                            System.currentTimeMillis() <= expiryTime
                        }
                    stories.addAll(batchStories)
                }

                emit(Response.Success(stories.sortedByDescending { it.timestamp }))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
            }
        }

    override suspend fun logStoryView(userID: String, storyID: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val storyRef = firestore.collection(Constants.COLLECTION_NAME_STORIES)
                    .document(storyID)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(storyRef)
                    if (snapshot.exists()) {
                        val views = snapshot.get("views") as? List<String> ?: emptyList()
                        if (userID !in views) {
                            transaction.update(storyRef, "views", FieldValue.arrayUnion(userID))
                            transaction.update(storyRef, "viewCount", FieldValue.increment(1))
                        }
                    }
                }.await()

                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
            }
        }

    override suspend fun deleteStory(storyID: String, userID: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                firestore.collection(Constants.COLLECTION_NAME_STORIES)
                    .document(storyID)
                    .delete()
                    .await()

                firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .document(userID)
                    .collection("stories")
                    .document(storyID)
                    .delete()
                    .await()

                val storageRef =
                    storage.reference.child("${Constants.COLLECTION_NAME_STORIES}/$userID/$storyID")
                val listResult = storageRef.listAll().await()
                for (item in listResult.items) {
                    item.delete().await()
                }

                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
            }
        }
}