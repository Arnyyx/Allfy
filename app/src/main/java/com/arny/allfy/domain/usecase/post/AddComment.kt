package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddComment @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(
        postID: String,
        userID: String,
        content: String,
        parentCommentID: String? = null
    ): Flow<Response<Boolean>> {
        return repository.addComment(postID, userID, content, parentCommentID)
    }
}