package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class GetPostByID @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postID: String) = repository.getPostByID(postID)
}