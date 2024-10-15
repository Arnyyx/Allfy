package com.arny.allfy.domain.repository

import com.arny.allfy.domain.model.Post
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(userID: String): Flow<Response<List<Post>>>
    fun uploadPost(post: Post): Flow<Response<Boolean>>
}