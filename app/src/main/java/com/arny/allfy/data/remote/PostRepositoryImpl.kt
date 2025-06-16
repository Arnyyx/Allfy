package com.arny.allfy.data.remote

import KeywordExtractor.extractKeywords
import android.content.Context
import android.net.Uri
import android.util.Log
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.MediaItem
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.retry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context,
    private val recommendationApi: RecommendationApi,
) : PostRepository {

    override suspend fun getFeedPosts(
        currentUser: String,
        lastVisible: Post?,
        limit: Int
    ): Flow<Response<List<Post>>> = flow {
        emit(Response.Loading)
        try {
            val response = retry {
                recommendationApi.getRecommendations(currentUser, limit * 2).also {
                    if (!it.isSuccessful) throw Exception("Server is offline ${it.code()}: ${it.message()}")
                }
            }

            if (response.isSuccessful) {
                val recommendationResponse = response.body()
                val recommendedPosts = recommendationResponse?.posts ?: emptyList()

                if (recommendedPosts.isEmpty()) {
                    emit(Response.Success(emptyList()))
                    return@flow
                }

                val recommendedPostIds = recommendedPosts.map { it.postId }

                val startIndex = if (lastVisible != null) {
                    recommendedPostIds.indexOfFirst { it == lastVisible.postID }.takeIf { it >= 0 }
                        ?.let { it + 1 } ?: 0
                } else {
                    0
                }

                val paginatedPostIds = recommendedPostIds.drop(startIndex).take(limit)

                if (paginatedPostIds.isEmpty()) {
                    emit(Response.Success(emptyList()))
                    return@flow
                }

                val chunkedPostIds =
                    paginatedPostIds.chunked(10)
                val allPosts = mutableListOf<Post>()

                for (chunk in chunkedPostIds) {
                    val snapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    val posts = snapshot.toObjects(Post::class.java)
                    allPosts.addAll(posts)
                }

                // Lấy dữ liệu user cho các postOwner
                val userIds = allPosts.map { it.postOwnerID }.distinct()
                val users = if (userIds.isNotEmpty()) {
                    val userDocs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                        .whereIn(FieldPath.documentId(), userIds)
                        .get()
                        .await()
                    userDocs.associate { it.id to it.toObject(User::class.java) }
                } else {
                    emptyMap()
                }

                val sortedPosts = paginatedPostIds.mapNotNull { postId ->
                    val post = allPosts.find { it.postID == postId }
                    val recommendation = recommendedPosts.find { it.postId == postId }
                    recommendation?.let {
                        post?.copy(
                            postOwner = users[post.postOwnerID] ?: User(),
                            score = it.score,
                            reason = it.reason
                        )
                    }
                }

                emit(Response.Success(sortedPosts))
            } else {
                emit(Response.Error("Failed to fetch recommendations: ${response.message()}"))
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
                    keywords = keywords,
                )

                val postMap = initialPost.let {
                    mapOf(
                        "postID" to it.postID,
                        "postOwnerID" to it.postOwnerID,
                        "mediaItems" to emptyList<MediaItem>(),
                        "caption" to it.caption,
                        "timestamp" to it.timestamp,
                        "likes" to it.likes,
                        "commentCount" to it.commentCount,
                        "keywords" to it.keywords
                    )
                }

                firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .document(postID)
                    .set(postMap)
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

                // Update post with media items
                val postWithMedia = postMap + mapOf("mediaItems" to mediaItems)
                firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .document(postID)
                    .set(postWithMedia)
                    .await()

                // Store post reference in user's posts collection
                val postRef = mapOf(
                    "postId" to postID,
                    "timestamp" to initialPost.timestamp
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

    override suspend fun editPost(
        postID: String,
        userID: String,
        newCaption: String,
        newImageUris: List<Uri>,
        mediaItemsToRemove: List<String>
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

            val post = postSnapshot.toObject(Post::class.java)
            if (post?.postOwnerID != userID) {
                emit(Response.Error("You are not authorized to edit this post"))
                return@flow
            }
            val existingMediaItems = post.mediaItems.toMutableList()
            for (mediaUrl in mediaItemsToRemove) {
                try {
                    val storageRef = storage.getReferenceFromUrl(mediaUrl)
                    storageRef.delete().await()
                    existingMediaItems.removeAll { it.url == mediaUrl }
                } catch (e: Exception) {
                    Log.e("PostRepositoryImpl", "Failed to delete media item: $mediaUrl, $e")
                }
            }
            val newMediaItems = if (newImageUris.isNotEmpty()) {
                newImageUris
                    .filter { it.scheme == "content" || it.scheme == "file" } // Filter local URIs
                    .map { uri ->
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
            val updatedMediaItems = (existingMediaItems + newMediaItems).distinctBy { it.url }
            val updatedKeywords = extractKeywords(newCaption)
            val updatedPostMap = mapOf(
                "caption" to newCaption,
                "mediaItems" to updatedMediaItems,
                "keywords" to updatedKeywords
            )
            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .update(updatedPostMap)
                .await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "editPost: $e")
            emit(Response.Error(e.localizedMessage ?: "Error editing post"))
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
                if (post != null) {
                    // Fetch user data for postOwner
                    val userSnapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                        .document(post.postOwnerID)
                        .get()
                        .await()
                    val user = userSnapshot.toObject(User::class.java) ?: User()
                    Response.Success(post.copy(postOwner = user))
                } else {
                    Response.Error("Post not found")
                }
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

            val userIds = allPosts.map { it.postOwnerID }.distinct()
            val users = if (userIds.isNotEmpty()) {
                val userDocs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .whereIn(FieldPath.documentId(), userIds)
                    .get()
                    .await()
                userDocs.associate { it.id to it.toObject(User::class.java) }
            } else {
                emptyMap()
            }

            val sortedPosts = postIDs.mapNotNull { id ->
                allPosts.find { it.postID == id }?.copy(postOwner = users[id] ?: User())
            }
            emit(Response.Success(sortedPosts))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }

    override suspend fun getComments(
        postID: String,
        lastVisible: Comment?,
        limit: Int
    ): Flow<Response<List<Comment>>> = flow {
        emit(Response.Loading)
        try {
            val baseQuery = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val finalQuery = if (lastVisible != null) {
                baseQuery.startAfter(lastVisible.timestamp)
            } else {
                baseQuery
            }

            val snapshot = finalQuery.get().await()

            if (snapshot.isEmpty) {
                emit(Response.Success(emptyList(), hasMore = false))
                return@flow
            }

            val comments = snapshot.toObjects(Comment::class.java)
                .filter { it.commentOwnerID.isNotEmpty() }

            if (comments.isEmpty()) {
                emit(Response.Success(emptyList(), hasMore = false))
                return@flow
            }

            val userIds = comments.map { it.commentOwnerID }.distinct()
            val userDataMap = if (userIds.isNotEmpty()) {
                val userSnapshots = firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .whereIn(FieldPath.documentId(), userIds)
                    .get()
                    .await()
                userSnapshots.associate { doc ->
                    doc.id to Pair(
                        doc.getString("username") ?: "",
                        doc.getString("imageUrl") ?: ""
                    )
                }
            } else {
                emptyMap()
            }

            val fullComments = comments.mapNotNull { comment ->
                val (username, imageUrl) = userDataMap[comment.commentOwnerID] ?: ("Unknown" to "")
                if (username.isEmpty() && userDataMap.containsKey(comment.commentOwnerID)) {
                    null
                } else {
                    comment.copy(
                        commentOwnerUserName = username,
                        commentOwnerProfilePicture = imageUrl
                    )
                }
            }

            val hasMore = fullComments.size == limit
            emit(Response.Success(fullComments, hasMore = hasMore))
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "getComments: $e")
            emit(Response.Error(e.message ?: "Failed to load comments"))
        }
    }

    override suspend fun addComment(
        postID: String,
        commentOwnerID: String,
        content: String,
        parentCommentID: String?,
        imageUri: Uri?
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            // Check if post belongs to the user
            val postSnapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .get()
                .await()
            if (postSnapshot.exists()) {
                val post = postSnapshot.toObject(Post::class.java)
                if (post?.postOwnerID == commentOwnerID) {
                    emit(Response.Error("Cannot comment on own post"))
                    return@flow
                }
            }

            val commentID = UUID.randomUUID().toString()
            var imageUrl: String? = null

            if (imageUri != null) {
                imageUrl = uploadCommentImage(postID, commentID, imageUri)
            }

            val commentData = mapOf(
                "commentId" to commentID,
                "content" to content,
                "timestamp" to Timestamp.now(),
                "imageUrl" to imageUrl
            )

            val interactionRef = firestore.collection("users")
                .document(commentOwnerID)
                .collection("interactions")
                .document(postID)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(interactionRef)
                if (snapshot.exists()) {
                    transaction.update(
                        interactionRef, mapOf(
                            "comments" to FieldValue.arrayUnion(commentData),
                            "interactionScore" to FieldValue.increment(1.5),
                            "lastUpdated" to Timestamp.now()
                        )
                    )
                } else {
                    transaction.set(
                        interactionRef, mapOf(
                            "postId" to postID,
                            "views" to emptyList<Map<String, Any>>(),
                            "likes" to emptyList<Map<String, Any>>(),
                            "comments" to listOf(commentData),
                            "interactionScore" to 1.5,
                            "lastUpdated" to Timestamp.now()
                        )
                    )
                }

                transaction.set(
                    firestore.collection(Constants.COLLECTION_NAME_POSTS)
                        .document(postID)
                        .collection("comments")
                        .document(commentID),
                    mapOf(
                        "commentID" to commentID,
                        "commentOwnerID" to commentOwnerID,
                        "content" to content,
                        "imageUrl" to imageUrl,
                        "timestamp" to Timestamp.now(),
                        "likes" to emptyList<String>(),
                        "parentCommentID" to parentCommentID
                    )
                )

                transaction.update(
                    firestore.collection(Constants.COLLECTION_NAME_POSTS).document(postID),
                    "commentCount", FieldValue.increment(1)
                )
            }.await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Error adding comment"))
        }
    }

    private suspend fun uploadCommentImage(postID: String, commentID: String, uri: Uri): String {
        try {
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.startsWith("image/") == true -> ".jpg"
                else -> ".jpg"
            }
            val fileName = "${System.currentTimeMillis()}$extension"
            val storageRef =
                storage.reference.child("${Constants.COLLECTION_NAME_POSTS}/$postID/comments/$commentID/$fileName")
            storageRef.putFile(uri).await()
            return storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "uploadCommentImage: $e")
            throw e
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
                // Check if post exists and get owner
                val postSnapshot = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                    .document(postID)
                    .get()
                    .await()

                if (!postSnapshot.exists()) {
                    emit(Response.Error("Post not found"))
                    return@flow
                }

                val postOwnerID = postSnapshot.getString("postOwnerID")
                if (postOwnerID == userID) {
                    emit(Response.Success(true)) // Silently succeed for own posts
                    return@flow
                }

                val interactionRef = firestore.collection("users")
                    .document(userID)
                    .collection("interactions")
                    .document(postID)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(interactionRef)
                    val viewData = mapOf(
                        "timestamp" to Timestamp.now(),
                        "duration" to 0
                    )

                    if (snapshot.exists()) {
                        val views = snapshot.get("views") as? List<Map<String, Any>> ?: emptyList()
                        // Check if view already exists within last minute to prevent duplicates
                        val recentView = views.any { view ->
                            val ts = view["timestamp"] as? Timestamp
                            ts != null && ts.toDate().time > System.currentTimeMillis() - 60000
                        }

                        if (!recentView) {
                            transaction.update(
                                interactionRef, mapOf(
                                    "views" to FieldValue.arrayUnion(viewData),
                                    "interactionScore" to FieldValue.increment(0.5),
                                    "lastUpdated" to Timestamp.now()
                                )
                            )
                        } else {
                        }
                    } else {
                        transaction.set(
                            interactionRef, mapOf(
                                "postId" to postID,
                                "views" to listOf(viewData),
                                "likes" to emptyList<Map<String, Any>>(),
                                "comments" to emptyList<Map<String, Any>>(),
                                "interactionScore" to 0.5,
                                "lastUpdated" to Timestamp.now()
                            )
                        )
                    }
                }.await()

                emit(Response.Success(true))
            } catch (e: Exception) {
                Log.e("PostRepositoryImpl", "logPostView: $e")
                emit(Response.Error(e.message ?: "Error logging view"))
            }
        }

    override suspend fun toggleLikePost(post: Post, userID: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val postRef = firestore.collection("posts").document(post.postID)
                val interactionRef = firestore.collection("users")
                    .document(userID)
                    .collection("interactions")
                    .document(post.postID)

                firestore.runTransaction { transaction ->
                    val postSnapshot = transaction.get(postRef)
                    val currentLikes = postSnapshot.get("likes") as? List<String> ?: emptyList()
                    val isLiked = userID in currentLikes
                    val updatedLikes = if (isLiked) currentLikes - userID else currentLikes + userID

                    transaction.update(postRef, "likes", updatedLikes)

                    if (post.postOwnerID != userID) {
                        val interactionSnapshot = transaction.get(interactionRef)
                        val likeData = mapOf("timestamp" to Timestamp.now())

                        if (interactionSnapshot.exists()) {
                            val likes = interactionSnapshot.get("likes") as? List<Map<String, Any>>
                                ?: emptyList()
                            val recentLike = likes.any { like ->
                                val ts = like["timestamp"] as? Timestamp
                                ts != null && ts.toDate().time > System.currentTimeMillis() - 60000
                            }

                            if (!recentLike || isLiked) {
                                transaction.update(
                                    interactionRef, mapOf(
                                        "likes" to if (isLiked) FieldValue.arrayRemove(likeData) else FieldValue.arrayUnion(
                                            likeData
                                        ),
                                        "interactionScore" to FieldValue.increment(if (isLiked) -1.0 else 1.0),
                                        "lastUpdated" to Timestamp.now()
                                    )
                                )
                            }
                        } else if (!isLiked) {
                            transaction.set(
                                interactionRef, mapOf(
                                    "postId" to post.postID,
                                    "views" to emptyList<Map<String, Any>>(),
                                    "likes" to listOf(likeData),
                                    "comments" to emptyList<Map<String, Any>>(),
                                    "interactionScore" to 1.0,
                                    "lastUpdated" to Timestamp.now()
                                )
                            )
                        }
                    }
                }.await()

                emit(Response.Success(true))
            } catch (e: Exception) {
                Log.e("PostRepositoryImpl", "toggleLikePost: $e")
                emit(Response.Error(e.message ?: "Error toggling like"))
            }
        }
}