package com.arny.allfy.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.MediaItem
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context
) : PostRepository {

    override fun getFeedPosts(
        currentUser: String,
        lastVisible: Post?,
        limit: Int
    ): Flow<Response<List<Post>>> = flow {
        emit(Response.Loading)
        try {
            val query = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .whereNotEqualTo("postOwnerID", currentUser)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val finalQuery = if (lastVisible != null) {
                query.startAfter(lastVisible.timestamp)
            } else {
                query
            }

            val snapshot = finalQuery.get().await()
            val posts = snapshot.toObjects(Post::class.java)
            emit(Response.Success(posts))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }


    override fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val postID = firestore.collection(Constants.COLLECTION_NAME_POSTS).document().id

            val initialPost = post.copy(
                postID = postID,
                mediaItems = emptyList()
            )
            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .set(initialPost)
                .await()

            val mediaItems = imageUris.map { uri ->
                val mimeType = context.contentResolver.getType(uri)
                val mediaType = when {
                    mimeType?.startsWith("image/") == true -> "image"
                    mimeType?.startsWith("video/") == true -> "video"
                    mimeType?.startsWith("audio/") == true -> "audio"
                    else -> "image"
                }
                val mediaUrl = uploadImageToFirebase(postID, uri)
                MediaItem(url = mediaUrl, mediaType = mediaType, thumbnailUrl = null)
            }

            val postWithMedia = initialPost.copy(mediaItems = mediaItems)
            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .set(postWithMedia)
                .await()

            val postRef = mapOf(
                "postId" to postID,
                "timestamp" to postWithMedia.timestamp
            )
            firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(post.postOwnerID)
                .collection("posts")
                .document(postID)
                .set(postRef)
                .await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun deletePost(postID: String, currentUserID: String): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .delete()
                .await()

            firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(currentUserID)
                .collection("posts")
                .document(postID)
                .delete()
                .await()

            storage.reference.child("${Constants.COLLECTION_NAME_POSTS}/$postID").delete().await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getPostByID(postID: String): Flow<Response<Post>> = flow {
        emit(Response.Loading)
        try {
            val snapshot =
                firestore.collection(Constants.COLLECTION_NAME_POSTS).document(postID).get().await()

            val response = if (snapshot.exists()) {
                val post = snapshot.toObject(Post::class.java)
                Response.Success(post!!)
            } else {
                Response.Error("Post not found")
            }
            emit(response)
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }

    override fun toggleLikePost(post: Post, userID: String): Flow<Response<Post>> = flow {
        emit(Response.Loading)
        try {
            val isLiked = post.likes.contains(userID)
            val updatedLikes = if (isLiked) {
                post.likes - userID
            } else {
                post.likes + userID
            }
            val updatedPost = post.copy(likes = updatedLikes)
            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(post.postID)
                .set(updatedPost)
                .await()

            emit(Response.Success(updatedPost))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getComments(postID: String): Flow<Response<List<Comment>>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .get()
                .await()

            if (snapshot.exists()) {
                val commentsData =
                    snapshot.get("comments") as? List<HashMap<String, Any>> ?: emptyList()
                if (commentsData.isEmpty()) {
                    emit(Response.Success(emptyList()))
                    return@flow
                }

                val baseComments = commentsData.mapNotNull { commentMap ->
                    try {
                        Triple(
                            commentMap["commentID"] as? String ?: return@mapNotNull null,
                            commentMap["commentOwnerID"] as? String ?: return@mapNotNull null,
                            Comment(
                                commentID = commentMap["commentID"] as? String
                                    ?: return@mapNotNull null,
                                commentOwnerID = commentMap["commentOwnerID"] as? String
                                    ?: return@mapNotNull null,
                                content = commentMap["content"] as? String
                                    ?: return@mapNotNull null,
                                timestamp = commentMap["timestamp"] as? Timestamp ?: Timestamp.now()
                            )
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                val userIds = baseComments.map { it.second }.distinct()

                val userSnapshots = firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .whereIn(FieldPath.documentId(), userIds)
                    .get()
                    .await()

                val userDataMap = userSnapshots.documents.associate { doc ->
                    doc.id to Pair(
                        doc.getString("username"),
                        doc.getString("imageUrl")
                    )
                }

                val fullComments = baseComments.map { (_, userId, baseComment) ->
                    val (username, imageUrl) = userDataMap[userId] ?: ("" to "")
                    baseComment.copy(
                        commentOwnerUserName = username ?: "",
                        commentOwnerProfilePicture = imageUrl ?: ""
                    )
                }.sortedByDescending { it.timestamp }
                emit(Response.Success(fullComments))
            } else {
                emit(Response.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun addComment(
        postID: String,
        commentOwnerID: String,
        content: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val newComment = hashMapOf(
                "commentID" to UUID.randomUUID().toString(),
                "commentOwnerID" to commentOwnerID,
                "content" to content,
                "timestamp" to Timestamp.now()
            )

            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .update("comments", FieldValue.arrayUnion(newComment))
                .await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    private suspend fun uploadImageToFirebase(postID: String, uri: Uri): String {
        try {
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.startsWith("image/") == true -> ".jpg"
                mimeType?.startsWith("video/") == true -> ".mp4"
                mimeType?.startsWith("audio/") == true -> ".mp3"
                else -> ".jpg"
            }
            val fileName = "${System.currentTimeMillis()}$extension"
            val storageRef =
                storage.reference.child("${Constants.COLLECTION_NAME_POSTS}/$postID/$fileName")
            storageRef.putFile(uri).await()
            return storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "uploadImageToFirebase: $e")
            throw e
        }
    }
}