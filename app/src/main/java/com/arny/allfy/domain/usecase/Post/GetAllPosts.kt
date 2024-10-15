package com.arny.allfy.domain.usecase.Post

import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class GetAllPosts @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(userID: String) = repository.getAllPosts(userID)
}