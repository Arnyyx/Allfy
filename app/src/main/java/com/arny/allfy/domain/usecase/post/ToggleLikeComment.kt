package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class ToggleLikeComment @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postID: String, commentID: String, userID: String) =
        repository.toggleLikeComment(postID, commentID, userID)

}