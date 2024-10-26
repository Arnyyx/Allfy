package com.arny.allfy.domain.usecase.Post

import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ToggleLikePost @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(post: Post, userID: String): Flow<Response<Post>> {
        return repository.toggleLikePost(post, userID)
    }
}


