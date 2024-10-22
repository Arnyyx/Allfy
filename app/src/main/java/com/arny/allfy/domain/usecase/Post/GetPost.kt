package com.arny.allfy.domain.usecase.Post

import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class GetPost @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(postID: String) = repository.getPost(postID)
}