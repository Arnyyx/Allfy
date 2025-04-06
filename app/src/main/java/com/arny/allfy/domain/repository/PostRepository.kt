package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getFeedPosts(
        currentUser: String,
        lastVisible: Post? = null,
        limit: Int = Constants.POST_LIMIT
    ): Flow<Response<List<Post>>>

    suspend fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>>
    suspend fun deletePost(postID: String, currentUserID: String): Flow<Response<Boolean>>
    suspend fun getPostByID(postID: String): Flow<Response<Post>>
    suspend fun getPostsByIDs(postIDs: List<String>): Flow<Response<List<Post>>>

    suspend fun toggleLikePost(post: Post, userID: String): Flow<Response<Boolean>>
    suspend fun getComments(postID: String): Flow<Response<List<Comment>>>
    suspend fun addComment(
        postID: String,
        commentOwnerID: String,
        content: String,
        parentCommentID: String? = null
    ): Flow<Response<Boolean>>

    suspend fun toggleLikeComment(
        postID: String,
        commentID: String,
        userID: String
    ): Flow<Response<Comment>>

    suspend fun logPostView(userID: String, postID: String): Flow<Response<Boolean>>
}