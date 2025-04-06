package com.arny.allfy.data.remote

import KeywordExtractor.extractKeywords
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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context
) : PostRepository {

    override suspend fun getFeedPosts(
        currentUser: String,
        lastVisible: Post?,
        limit: Int
    ): Flow<Response<List<Post>>> = flow {
        emit(Response.Loading)
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://recommendationserver.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(RecommendationApi::class.java)
            val response = api.getRecommendations(currentUser, limit)

            if (response.isSuccessful) {
                val recommendedPostIds = response.body()?.postIds ?: emptyList()

                if (recommendedPostIds.isEmpty()) {
                    emit(Response.Success(emptyList()))
                    return@flow
                }

                val query = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .whereIn("postID", recommendedPostIds)
                    .orderBy("timestamp", Query.Direction.DESCENDING)

                val finalQuery = if (lastVisible != null) {
                    query.startAfter(lastVisible.timestamp)
                } else {
                    query
                }

                val snapshot = finalQuery.get().await()
                val posts = snapshot.toObjects(Post::class.java)
                emit(Response.Success(posts))
            } else {
                emit(Response.Error("Failed to fetch recommendations"))
            }
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val postID = firestore.collection(Constants.COLLECTION_NAME_POSTS).document().id

                val keywords = extractKeywords(post.caption)
                val initialPost = post.copy(
                    postID = postID,
                    mediaItems = emptyList(),
                    keywords = keywords
                )
                firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .document(postID)
                    .set(initialPost)
                    .await()

                val mediaItems = if (imageUris.isNotEmpty()) {
                    imageUris.map { uri ->
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
                } else {
                    emptyList()
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

    override suspend fun deletePost(
        postID: String,
        currentUserID: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val postSnapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .get()
                .await()

            if (!postSnapshot.exists()) {
                emit(Response.Error("Post not found"))
                return@flow
            }

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

            val storageRef = storage.reference.child("${Constants.COLLECTION_NAME_POSTS}/$postID")
            val listResult = storageRef.listAll().await()

            for (item in listResult.items) {
                item.delete().await()
            }

            val commentsSnapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .collection("comments")
                .get()
                .await()

            for (commentDoc in commentsSnapshot.documents) {
                firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .document(postID)
                    .collection("comments")
                    .document(commentDoc.id)
                    .delete()
                    .await()
            }

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun getPostByID(postID: String): Flow<Response<Post>> = flow {
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

    override suspend fun getPostsByIDs(postIDs: List<String>): Flow<Response<List<Post>>> = flow {
        emit(Response.Loading)
        try {
            if (postIDs.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }

            val chunkedPostIDs = postIDs.chunked(10)
            val allPosts = mutableListOf<Post>()

            for (chunk in chunkedPostIDs) {
                val snapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                val posts = snapshot.toObjects(Post::class.java)
                allPosts.addAll(posts)
            }

            val sortedPosts = postIDs.mapNotNull { id -> allPosts.find { it.postID == id } }
            emit(Response.Success(sortedPosts))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }


    override suspend fun getComments(postID: String): Flow<Response<List<Comment>>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val comments = if (snapshot.isEmpty) {
                emptyList()
            } else {
                snapshot.toObjects(Comment::class.java)
            }

            if (comments.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }

            val userIds = comments.map { it.commentOwnerID }.distinct()

            val userSnapshots = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .await()

            val userDataMap = userSnapshots.documents.associate { doc ->
                doc.id to Pair(doc.getString("username"), doc.getString("imageUrl"))
            }

            val fullComments = comments.map { comment ->
                val (username, imageUrl) = userDataMap[comment.commentOwnerID] ?: ("" to "")
                comment.copy(
                    commentOwnerUserName = username ?: "",
                    commentOwnerProfilePicture = imageUrl ?: ""
                )
            }
            emit(Response.Success(fullComments))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun addComment(
        postID: String,
        commentOwnerID: String,
        content: String,
        parentCommentID: String?
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val commentID = UUID.randomUUID().toString()
            val newComment = hashMapOf(
                "commentID" to commentID,
                "commentOwnerID" to commentOwnerID,
                "content" to content,
                "timestamp" to Timestamp.now(),
                "likes" to emptyList<String>(),
                "parentCommentID" to parentCommentID
            )

            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .collection("comments")
                .document(commentID)
                .set(newComment)
                .await()

            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .update("commentCount", FieldValue.increment(1))
                .await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun toggleLikeComment(
        postID: String,
        commentID: String,
        userID: String
    ): Flow<Response<Comment>> = flow {
        emit(Response.Loading)
        try {
            val commentRef = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .collection("comments")
                .document(commentID)

            val snapshot = commentRef.get().await()
            if (snapshot.exists()) {
                val comment = snapshot.toObject(Comment::class.java)!!
                val isLiked = comment.likes.contains(userID)
                val updatedLikes = if (isLiked) {
                    comment.likes - userID
                } else {
                    comment.likes + userID
                }
                val updatedComment = comment.copy(likes = updatedLikes)
                commentRef.set(updatedComment).await()

                val userSnapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .document(comment.commentOwnerID)
                    .get()
                    .await()

                val finalComment = updatedComment.copy(
                    commentOwnerUserName = userSnapshot.getString("username") ?: "",
                    commentOwnerProfilePicture = userSnapshot.getString("imageUrl") ?: ""
                )
                emit(Response.Success(finalComment))
            } else {
                emit(Response.Error("Comment not found"))
            }
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun logPostView(userID: String, postID: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val activityId = "${userID}_${postID}_view"
                val activityRef = firestore.collection("userActivity").document(activityId)

                activityRef.set(
                    mapOf(
                        "userId" to userID,
                        "postId" to postID,
                        "type" to "view",
                        "timestamp" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "Error logging view"))
            }
        }

    override suspend fun toggleLikePost(post: Post, userID: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val postRef = firestore.collection("posts").document(post.postID)
                val activityId = "${userID}_${post.postID}_like"
                val activityRef = firestore.collection("userActivity").document(activityId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentLikes = snapshot.get("likes") as? List<String> ?: emptyList()
                    val isLiked = userID in currentLikes
                    val updatedLikes = if (isLiked) currentLikes - userID else currentLikes + userID

                    transaction.update(postRef, "likes", updatedLikes)
                    if (isLiked) {
                        transaction.delete(activityRef)
                    } else {
                        transaction.set(
                            activityRef,
                            mapOf(
                                "userId" to userID,
                                "postId" to post.postID,
                                "type" to "like",
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "Error toggling like"))
            }
        }
}