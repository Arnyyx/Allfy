package com.arny.allfy.domain.usecase.Post

import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class UploadPost @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(post: Post) = repository.uploadPost(post)
}