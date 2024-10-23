package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.Post
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(
        userID: String,
        lastVisible: Post? = null,
        limit: Int = Constants.POST_LIMIT
    ): Flow<Response<List<Post>>>

    fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>>
    fun getPostByID(postID: String): Flow<Response<Post>>
     suspend fun updatePost(post: Post)
}