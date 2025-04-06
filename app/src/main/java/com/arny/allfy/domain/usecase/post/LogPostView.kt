package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class LogPostView @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(userID: String, postID: String) =
        repository.logPostView(userID, postID)
}