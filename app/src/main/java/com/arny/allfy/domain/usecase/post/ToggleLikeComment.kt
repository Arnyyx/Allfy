package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ToggleLikeComment @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(
        postID: String,
        commentID: String,
        userID: String
    ): Flow<Response<Comment>> {
        return repository.toggleLikeComment(postID, commentID, userID)
    }
}