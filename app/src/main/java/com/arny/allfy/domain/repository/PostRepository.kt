package com.arny.allfy.domain.repository

import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import android.net.Uri

interface PostRepository {
    suspend fun getFeedPosts(
        currentUser: String,
        lastVisible: Post? = null,
        limit: Int = 10
    ): Flow<Response<List<Post>>>

    suspend fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>>
    suspend fun deletePost(postID: String, currentUserID: String): Flow<Response<Boolean>>
    suspend fun getPostByID(postID: String): Flow<Response<Post>>
    suspend fun getPostsByIDs(postIDs: List<String>): Flow<Response<List<Post>>>

    suspend fun getComments(
        postID: String,
        lastVisible: Comment? = null,
        limit: Int = 10
    ): Flow<Response<List<Comment>>>

    suspend fun addComment(
        postID: String,
        commentOwnerID: String,
        content: String,
        parentCommentID: String?,
        imageUri: Uri?
    ): Flow<Response<Boolean>>

    suspend fun toggleLikeComment(
        postID: String,
        commentID: String,
        userID: String
    ): Flow<Response<Comment>>

    suspend fun logPostView(userID: String, postID: String): Flow<Response<Boolean>>
    suspend fun toggleLikePost(post: Post, userID: String): Flow<Response<Boolean>>
}