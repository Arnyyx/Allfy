package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.Post
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(userID: String): Flow<Response<List<Post>>>
    fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>>
}